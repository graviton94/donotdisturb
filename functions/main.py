"""Do Not Disturb! Cloud Functions (Blaze, Python). Deployed by .github/workflows/functions.yml.
Same login pattern as the sister app mint-and-friends.

- login(provider, token): checks a Kakao access token with Kakao and returns a Firebase custom token.
    uid = "kakao:<Kakao user id>". With KAKAO_APP_ID set, tokens issued to another Kakao app are refused.
- deleteAccount(): deletes the signed-in user's cloud save (users/{uid}) and the Firebase user (Play policy: in-app deletion).
"""
import datetime
import json
import os
import urllib.error
import urllib.request

from firebase_admin import auth, firestore, initialize_app
from firebase_functions import https_fn, scheduler_fn

initialize_app()

REGION = 'us-central1'

# Our Kakao app's numeric id (Kakao Developers → app → App ID), from functions/.env written by the workflow.
KAKAO_APP_ID = (os.environ.get('KAKAO_APP_ID') or '').strip()
if KAKAO_APP_ID and not KAKAO_APP_ID.isdigit():
    print('KAKAO_APP_ID is not numeric: ignoring it')
    KAKAO_APP_ID = ''

_PROFILE_URLS = {'kakao': 'https://kapi.kakao.com/v2/user/me'}


class _NotOurToken(Exception):
    pass


def _get(url: str, token: str) -> dict:
    req = urllib.request.Request(url, headers={'Authorization': f'Bearer {token}'})
    with urllib.request.urlopen(req, timeout=8) as r:
        return json.load(r)


def _kakao_is_ours(token: str) -> None:
    if not KAKAO_APP_ID:
        return
    info = _get('https://kapi.kakao.com/v1/user/access_token_info', token)
    if str(info.get('app_id')) != KAKAO_APP_ID:
        raise _NotOurToken()


def _profile(provider: str, token: str) -> tuple[str, str | None, str | None]:
    """(provider user id, nickname, photo URL). Raises on a bad token."""
    if provider == 'kakao':
        _kakao_is_ours(token)
    body = _get(_PROFILE_URLS[provider], token)
    profile = (body.get('kakao_account') or {}).get('profile') or {}
    props = body.get('properties') or {}
    return (str(body['id']), profile.get('nickname') or props.get('nickname'),
            profile.get('profile_image_url') or props.get('profile_image'))


@https_fn.on_call(region=REGION, max_instances=10)
def login(req: https_fn.CallableRequest) -> dict:
    data = req.data if isinstance(req.data, dict) else {}
    provider, token = data.get('provider'), data.get('token')
    if provider == 'warm':  # the app wakes this instance while the title screen is up (cold start ~ several s)
        return {}
    if provider not in _PROFILE_URLS or not isinstance(token, str) or not token or len(token) > 4096:
        raise https_fn.HttpsError(https_fn.FunctionsErrorCode.INVALID_ARGUMENT, 'provider and token required')
    try:
        pid, name, photo = _profile(provider, token)
    except _NotOurToken as e:
        print('login: a kakao token made for another app')
        raise https_fn.HttpsError(https_fn.FunctionsErrorCode.UNAUTHENTICATED, 'token not accepted') from e
    except urllib.error.HTTPError as e:
        print(f'login: {provider} refused the token ({e.code})')
        raise https_fn.HttpsError(https_fn.FunctionsErrorCode.UNAUTHENTICATED, 'token not accepted') from e
    except Exception as e:
        print(f'login: {provider} check failed: {type(e).__name__}')
        raise https_fn.HttpsError(https_fn.FunctionsErrorCode.UNAVAILABLE, 'could not check the token') from e

    uid = f'{provider}:{pid}'[:128]
    fields = {'display_name': (name or '')[:100] or None}
    if photo and photo.startswith('http://'):
        photo = 'https://' + photo[len('http://'):]
    if photo and photo.startswith('https://'):
        fields['photo_url'] = photo
    try:
        auth.update_user(uid, **fields)
    except auth.UserNotFoundError:
        auth.create_user(uid=uid, **fields)
    return {'token': auth.create_custom_token(uid, {'provider': provider}).decode()}


@https_fn.on_call(region=REGION, max_instances=10)
def deleteAccount(req: https_fn.CallableRequest) -> dict:  # noqa: N802 (the name the app calls)
    if req.auth is None:
        raise https_fn.HttpsError(https_fn.FunctionsErrorCode.UNAUTHENTICATED, 'login required')
    uid = req.auth.uid
    db = firestore.client()
    db.collection('users').document(uid).delete()
    # weekly ranking entries of the last 10 weeks in every league (older boards are no longer shown)
    today = datetime.date.today()
    for w in range(10):
        y, wk, _ = (today - datetime.timedelta(weeks=w)).isocalendar()
        for league in range(5):
            db.collection('weekly').document(f'{y}-W{wk:02d}-L{league}').collection('scores').document(uid).delete()
            db.collection('weekly').document(f'{y}-W{wk:02d}-L{league}').collection('results').document(uid).delete()
    auth.delete_user(uid)
    return {'ok': True}


def _board(day: datetime.date) -> str:
    y, w, _ = day.isocalendar()
    return f'{y}-W{w:02d}-L0'


def _tier(rank: int) -> str:
    return 'first' if rank == 1 else 'top10' if rank <= 10 else 'top100' if rank <= 100 else 'joined'


@scheduler_fn.on_schedule(schedule='10 0 * * 1', timezone=scheduler_fn.Timezone('Asia/Seoul'), region=REGION)
def settleWeekly(event: scheduler_fn.ScheduledEvent) -> None:  # noqa: N802
    """Monday 00:10 KST: rank last week's daily-challenge board once and leave each player their result
    (weekly/{board}/results/{uid} = {rank, tier, of}); the app claims the reward through the core, once per board."""
    db = firestore.client()
    kst = datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=9))).date()
    board = _board(kst - datetime.timedelta(days=7))
    ref = db.collection('weekly').document(board)
    if (snap := ref.get()).exists and snap.to_dict().get('settled'):
        return
    docs = list(ref.collection('scores').order_by('scoreLog', direction=firestore.Query.DESCENDING).stream())
    batch, n = db.batch(), 0
    for rank, d in enumerate(docs, start=1):
        batch.set(ref.collection('results').document(d.id), {'rank': rank, 'tier': _tier(rank), 'of': len(docs)})
        n += 1
        if n % 400 == 0:
            batch.commit()
            batch = db.batch()
    batch.set(ref, {'settled': True, 'players': len(docs), 'at': firestore.SERVER_TIMESTAMP}, merge=True)
    batch.commit()
    print(f'settled {board}: {len(docs)} players')
