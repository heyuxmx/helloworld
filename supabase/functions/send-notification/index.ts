import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { create, getNumericDate } from 'https://deno.land/x/djwt@v2.2/mod.ts'

// 这是一个辅助函数，用来向谷歌换取一个有时效性的“访问令牌”
async function getGoogleAuthToken(serviceAccount: any) {
  const jwt = await create(
    { alg: 'RS256', typ: 'JWT' },
    {
      iss: serviceAccount.client_email,
      scope: 'https://www.googleapis.com/auth/cloud-platform',
      aud: 'https://oauth2.googleapis.com/token',
      exp: getNumericDate(3600), // 令牌有效期为 1 小时
      iat: getNumericDate(0),
    },
    serviceAccount.private_key
  )

  const response = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: jwt,
    }),
  })

  const data = await response.json()
  return data.access_token
}

// 这是函数被触发时运行的主程序
Deno.serve(async (req) => {
  try {
    // 1. 从触发器获取新创建的 post 数据
    const payload = await req.json()
    const postRecord = payload.record

    // 2. 从“保险箱”里取出我们设置的“特殊通行证”
    const serviceAccountJson = Deno.env.get('GOOGLE_SERVICE_ACCOUNT_KEY')
    if (!serviceAccountJson) {
      throw new Error('错误：在云端未找到 GOOGLE_SERVICE_ACCOUNT_KEY 这个秘密。')
    }
    const serviceAccount = JSON.parse(serviceAccountJson)
    const googleProjectId = serviceAccount.project_id

    // 3. 创建一个 Supabase 客户端，用来查询数据库
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    // 4. 判断应该把通知发给谁
    const authorId = postRecord.user_id
    const xiaoxuId = '67890'
    const xiaogaoId = '12345'
    let recipientId = ''
    let authorUsername = ''

    if (authorId === xiaoxuId) {
      recipientId = xiaogaoId
      authorUsername = 'xiaoxu'
    } else if (authorId === xiaogaoId) {
      recipientId = xiaoxuId
      authorUsername = 'xiaogao'
    } else {
      console.log(`作者 (ID: ${authorId}) 不是指定用户，无需发送通知。`)
      return new Response("Notification not required for this user.", { status: 200 })
    }

    // 5. 根据接收者的 ID，从 'users' 表里找到他的“通知地址”(fcm_token)
    const { data: recipient, error: recipientError } = await supabaseClient
      .from('users')
      .select('fcm_token')
      .eq('id', recipientId)
      .single()

    if (recipientError || !recipient || !recipient.fcm_token) {
      throw new Error(`错误：接收者 (ID: ${recipientId}) 未找到或其 fcm_token 为空。`)
    }
    const fcmToken = recipient.fcm_token

    // 6. 调用辅助函数，换取一个临时的“访问令牌”
    const authToken = await getGoogleAuthToken(serviceAccount)
    if (!authToken) {
      throw new Error("错误：向谷歌换取访问令牌失败。")
    }

    // 7. 准备新版 API 所需的通知内容
    const fcmPayload = {
      message: {
        token: fcmToken,
        notification: {
          title: '你的挚友有新动态啦！',
          body: `${authorUsername} 发布了一条新动态，快去看看吧！`,
        },
        data: {
          navigate_to_post_id: postRecord.id.toString(),
        },
      },
    }

    // 8. 把通知发送给谷歌的 Firebase 服务器 (使用新版 API 地址)
    const fcmUrl = `https://fcm.googleapis.com/v1/projects/${googleProjectId}/messages:send`
    const response = await fetch(fcmUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${authToken}`, // 使用有时效性的令牌
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(fcmPayload),
    })

    if (!response.ok) {
      const errorBody = await response.text()
      throw new Error(`FCM 请求失败: ${response.status} ${errorBody}`)
    }

    console.log(`通知已成功发送给用户 ${recipientId}`)

    // 9. 大功告成，返回成功信息
    return new Response(JSON.stringify({ success: true }), {
      headers: { 'Content-Type': 'application/json' },
      status: 200,
    })

  } catch (err) {
    // 如果任何一个环节出错，就在日志里记录错误
    console.error("函数执行出错:", err.message)
    return new Response(String(err?.message ?? err), { status: 500 })
  }
})