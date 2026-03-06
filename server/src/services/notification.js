const { GoogleAuth } = require('google-auth-library');
const db = require('../db');

const XIAOXU_ID = '67890';
const XIAOGAO_ID = '12345';

async function getAccessToken(serviceAccount) {
  const auth = new GoogleAuth({
    credentials: serviceAccount,
    scopes: ['https://www.googleapis.com/auth/cloud-platform'],
  });
  const client = await auth.getClient();
  const tokenResponse = await client.getAccessToken();
  return tokenResponse.token;
}

async function sendNotification(post) {
  const serviceAccountJson = process.env.GOOGLE_SERVICE_ACCOUNT_KEY;
  if (!serviceAccountJson) {
    console.warn('⚠️  GOOGLE_SERVICE_ACCOUNT_KEY 未配置，跳过推送通知');
    return;
  }

  const serviceAccount = JSON.parse(serviceAccountJson);
  const projectId = serviceAccount.project_id;
  const authorId = post.user_id;

  let recipientId, authorName;
  if (authorId === XIAOXU_ID) {
    recipientId = XIAOGAO_ID;
    authorName = '徐大王';
  } else if (authorId === XIAOGAO_ID) {
    recipientId = XIAOXU_ID;
    authorName = '高猪猪';
  } else {
    console.log(`用户 ${authorId} 不是指定用户，无需发送通知`);
    return;
  }

  const { rows } = await db.query('SELECT fcm_token FROM users WHERE id = $1', [recipientId]);
  if (!rows[0]?.fcm_token) {
    console.warn(`⚠️  接收方 ${recipientId} 没有 FCM Token，跳过推送`);
    return;
  }

  const accessToken = await getAccessToken(serviceAccount);

  const payload = {
    message: {
      token: rows[0].fcm_token,
      notification: {
        title: '你的挚友有新动态啦！',
        body: `${authorName} 发布了一条新动态，快去看看吧！`,
      },
      data: {
        navigate_to_post_id: post.id.toString(),
      },
    },
  };

  const response = await fetch(
    `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    }
  );

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`FCM 请求失败: ${response.status} ${errText}`);
  }

  console.log(`✅ 推送通知已发送给用户 ${recipientId}`);
}

module.exports = { sendNotification };
