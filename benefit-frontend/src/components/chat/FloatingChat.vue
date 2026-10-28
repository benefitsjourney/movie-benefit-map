<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Client } from '@stomp/stompjs'
import http from '@/plugins/axios'
import { useAuth } from '@/composables/useAuth'
import 'bootstrap-icons/font/bootstrap-icons.css'

const auth = useAuth()
const router = useRouter()

// WebSocket / STOMP
let stompClient = null
const conversationSubscriptions = new Map()

// 視窗狀態
const isOpen = ref(false)
const isMinimized = ref(false)
const isExpanded = ref(false)

// 聊天室列表與目前對話
const conversations = ref([])
const activeConversation = ref(null)

// 是否有未讀訊息
const hasUnreadMessages = computed(() =>
  conversations.value.some(
    conversation => Number(conversation.unreadCount) > 0
  )
)

// 訊息
const messages = ref([])
const messageContent = ref('')

// 載入狀態與錯誤訊息
const loadingConversations = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)
const errorMessage = ref('')
const sendError = ref('')

// 訊息列表 DOM
const messageList = ref(null)

// 登入狀態
const isLoggedIn = computed(() => auth.isLoggedIn.value)

// 目前會員 ID
const currentUserId = computed(() => {
  const user = auth.user.value

  return (
    user?.usersId ??
    user?.users_id ??
    user?.userId ??
    user?.id ??
    null
  )
})

// 取得 WebSocket 連線網址
function getWebSocketUrl() {
  // 本機開發：透過 Vite 的 /ws 代理連到 Spring Boot
  if (import.meta.env.DEV) {
    const protocol =
      window.location.protocol === 'https:' ? 'wss:' : 'ws:'

    return `${protocol}//${window.location.host}/ws`
  }

  // 正式環境：優先使用 API 設定中的主機
  const apiBaseUrl = import.meta.env.VITE_API_URL
  let apiUrl

  try {
    apiUrl = new URL(
      apiBaseUrl || 'https://movie-benefit-map.onrender.com',
      window.location.origin
    )
  } catch {
    apiUrl = new URL('https://movie-benefit-map.onrender.com')
  }

  // 若 API 設定是相對路徑，避免錯連到 GitHub Pages
  if (apiUrl.hostname.endsWith('github.io')) {
    apiUrl = new URL('https://movie-benefit-map.onrender.com')
  }

  const protocol = apiUrl.protocol === 'https:' ? 'wss:' : 'ws:'

  return `${protocol}//${apiUrl.host}/ws`
}

// 將訊息加入列表，避免 REST 與 WebSocket 重複顯示
function addMessageIfMissing(message) {
  if (!message) return

  if (
    message.messageId != null &&
    messages.value.some(
      item =>
        String(item.messageId) === String(message.messageId)
    )
  ) {
    return
  }

  messages.value.push(message)

  messages.value.sort(
    (a, b) => new Date(a.sentAt) - new Date(b.sentAt)
  )
}


function unsubscribeAllConversations() {
  conversationSubscriptions.forEach(subscription => {
    subscription.unsubscribe()
  })

  conversationSubscriptions.clear()
}

// 訂閱所有已知聊天室，聊天視窗關閉時也持續接收訊息
function subscribeToAllConversations() {
  if (!stompClient?.connected) return

  conversations.value.forEach(conversation => {
    const conversationId = conversation.conversationId
    if (!conversationId) return

    const key = String(conversationId)

    // 避免重複訂閱
    if (conversationSubscriptions.has(key)) return

    const subscription = stompClient.subscribe(
      `/topic/conversations/${conversationId}`,
      async frame => {
        try {
          const message = JSON.parse(frame.body)

          // 相容不同的訊息 JSON 結構
          const receivedConversationId =
            message.conversationId ??
            message.conversation?.conversationId

          if (
            String(receivedConversationId) !== key
          ) {
            return
          }

          const senderId =
            message.senderId ??
            message.sender?.usersId ??
            message.sender?.users_id

          const isMine =
            currentUserId.value != null &&
            senderId != null &&
            String(senderId) === String(currentUserId.value)

          // 更新聊天室最後訊息
          const item = conversations.value.find(
            c => String(c.conversationId) === key
          )

          if (item) {
            item.lastMessageContent = message.content
            item.lastMessageAt = message.sentAt
          }

          // 如果目前正在看這個聊天室，而且視窗沒有收合，
          // 收到對方訊息後直接標記已讀，不增加未讀數
          const isViewingConversation =
            isOpen.value &&
            !isMinimized.value &&
            activeConversation.value &&
            String(activeConversation.value.conversationId) === key

          if (!isMine && !isViewingConversation && item) {
            item.unreadCount =
              Number(item.unreadCount || 0) + 1
          }

          // 更新目前開啟的聊天室訊息
          if (
            isOpen.value &&
            activeConversation.value &&
            String(activeConversation.value.conversationId) === key
          ) {
            addMessageIfMissing(message)

            if (!isMinimized.value) {
              await scrollToBottom()
            }
          }

          // 對方傳來訊息且目前正在閱讀該聊天室，立即標記已讀
          if (!isMine && isViewingConversation) {
            try {
              await http.patch(
                `/conversations/${conversationId}/messages/read`
              )

              if (item) {
                item.unreadCount = 0
              }
            } catch (error) {
              console.error('即時標記已讀失敗：', error)
            }
          }
        } catch (error) {
          console.error('處理即時訊息失敗：', error)
        }
      }
    )

    conversationSubscriptions.set(key, subscription)
  })
}


// 建立 WebSocket 連線並訂閱聊天室

function connectWebSocket() {
  const token = localStorage.getItem('token')

  if (!token || !isLoggedIn.value) {
    return
  }

  if (stompClient) {
    if (stompClient.connected) {
      subscribeToAllConversations()
    }
    return
  }

  stompClient = new Client({
    brokerURL: getWebSocketUrl(),

    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },

    reconnectDelay: 5000,

    onConnect: () => {
      console.log('WebSocket 已連線')
      subscribeToAllConversations()
    },

    onStompError: frame => {
      console.error(
        'STOMP 錯誤：',
        frame.headers.message || frame.body
      )
    },

    onWebSocketError: error => {
      console.error('WebSocket 連線錯誤：', error)
    },
  })

  stompClient.activate()
}

// 載入聊天室列表
async function loadConversations() {
  if (!isLoggedIn.value) return

  loadingConversations.value = true
  errorMessage.value = ''

  try {
    const response = await http.get('/conversations')

    conversations.value = Array.isArray(response.data)
      ? response.data
      : []
      subscribeToAllConversations()
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      '無法載入聊天室列表，請稍後再試。'
  } finally {
    loadingConversations.value = false
  }
}

// 開啟聊天視窗
async function openChat() {
  if (!isLoggedIn.value) return

  isOpen.value = true
  isMinimized.value = false

  await loadConversations()
}

// 收合視窗
function minimizeChat() {
  isMinimized.value = true
}

// 關閉視窗
function closeChat() {
  isOpen.value = false
  isMinimized.value = false
  isExpanded.value = false
  activeConversation.value = null
  messages.value = []
  messageContent.value = ''
  errorMessage.value = ''
  sendError.value = ''
}

// 返回聊天室列表
async function backToList() {
  activeConversation.value = null
  messages.value = []
  messageContent.value = ''
  errorMessage.value = ''
  sendError.value = ''

  await loadConversations()
}

// 開啟聊天室
async function openConversation(conversation) {
  activeConversation.value = conversation
  messages.value = []
  messageContent.value = ''
  errorMessage.value = ''
  sendError.value = ''
  loadingMessages.value = true

  try {
    const response = await http.get(
      `/conversations/${conversation.conversationId}/messages`,
      {
        params: {
          page: 0,
          size: 100,
        },
      }
    )

    const data = response.data

    messages.value = (
      Array.isArray(data) ? data : data?.content || []
    ).sort(
      (a, b) => new Date(a.sentAt) - new Date(b.sentAt)
    )

    // 確保 WebSocket 已連線，並訂閱所有聊天室
    connectWebSocket()
    subscribeToAllConversations()

    // 標記訊息已讀
    await http.patch(
      `/conversations/${conversation.conversationId}/messages/read`
    )

    await loadConversations()
  } catch (error) {
    errorMessage.value =
    error.response?.data?.message ||
    '無法載入訊息，請稍後再試。'
  } finally {
    loadingMessages.value = false
    await scrollToBottom()
  }
}

// 開啟原本的完整聊天室頁面
function openFullConversation() {
    closeChat()
    router.push('/account/messages')
}

// 傳送訊息
async function sendMessage() {
  const content = messageContent.value.trim()

  if (
    !content ||
    sending.value ||
    !activeConversation.value
  ) {
    return
  }

  if (content.length > 2000) {
    sendError.value = '訊息長度不可超過 2000 字。'
    return
  }

  sending.value = true
  sendError.value = ''

  try {
    const conversationId =
      activeConversation.value.conversationId

    const response = await http.post(
      `/conversations/${conversationId}/messages`,
      { content }
    )

    addMessageIfMissing(response.data)
    messageContent.value = ''

    await scrollToBottom()
    await loadConversations()
  } catch (error) {
    sendError.value =
      error.response?.data?.message ||
      '訊息傳送失敗，請稍後再試。'
  } finally {
    sending.value = false
  }
}

// 判斷訊息是否由目前會員傳送
function isMyMessage(message) {
  const senderId =
    message.senderId ??
    message.sender?.usersId ??
    message.sender?.users_id ??
    message.sender?.userId ??
    message.sender?.id

  if (currentUserId.value == null || senderId == null) {
    return false
  }

  return String(senderId) === String(currentUserId.value)
}

// 捲動至最新訊息
async function scrollToBottom() {
  await nextTick()

  if (messageList.value) {
    messageList.value.scrollTop =
      messageList.value.scrollHeight
  }
}

watch(isExpanded, async () => {
  await scrollToBottom()
})

/**
 * 將訊息中的 HTTP/HTTPS 網址拆成可點擊連結與一般文字
 */

function getMessageParts(content) {
  const text = String(content ?? '')
  const parts = []

  // 擷取 HTTP / HTTPS 網址
  const urlPattern = /https?:\/\/[^\s<>"']+/gi

  let lastIndex = 0

  for (const match of text.matchAll(urlPattern)) {
    const startIndex = match.index
    const rawUrl = match[0]

    // 移除網址末尾的中英文標點符號
    let url = rawUrl.replace(/[.,!?;:，。！？；：、）】》」』]+$/u, '')

    if (!url) continue

    // 確認網址格式正確，只允許 HTTP / HTTPS
    try {
      const parsedUrl = new URL(url)

      if (!['http:', 'https:'].includes(parsedUrl.protocol)) {
        continue
      }
    } catch {
      continue
    }

    // 加入網址前面的普通文字
    if (startIndex > lastIndex) {
      parts.push({
        text: text.slice(lastIndex, startIndex),
        url: null,
      })
    }

    // 加入可點擊的網址
    parts.push({
      text: url,
      url: url,
    })

    // 更新位置，只跳過實際網址，不跳過後面的標點或文字
    lastIndex = startIndex + url.length
  }

  // 加入剩下的普通文字，包括標點符號與網址後面的內容
  if (lastIndex < text.length) {
    parts.push({
      text: text.slice(lastIndex),
      url: null,
    })
  }

  return parts.length ? parts : [{ text, url: null }]
}

// 格式化時間
function formatTime(value) {
  if (!value) return ''

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return ''

  const now = new Date()

  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-TW', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    })
  }

  return date.toLocaleDateString('zh-TW', {
    month: '2-digit',
    day: '2-digit',
  })
}

// 接收其他頁面要求開啟浮動聊天室的事件
async function handleOpenFloatingChat(event) {
  const conversation = event.detail?.conversation

  await openChat()

  if (conversation) {
    await openConversation(conversation)
  }
}


onMounted(() => {
  window.addEventListener(
    'open-floating-chat',
    handleOpenFloatingChat
  )

  if (isLoggedIn.value) {
    connectWebSocket()
    loadConversations()
  }
})

// 登入後開始接收通知；登出後關閉連線
watch(isLoggedIn, async loggedIn => {
  if (loggedIn) {
    await loadConversations()
    connectWebSocket()
  } else {
    unsubscribeAllConversations()

    if (stompClient) {
      await stompClient.deactivate()
      stompClient = null
    }

    conversations.value = []
    activeConversation.value = null
    messages.value = []
  }
})

onUnmounted(() => {
  window.removeEventListener(
    'open-floating-chat',
    handleOpenFloatingChat
  )

  unsubscribeAllConversations()

  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
})

</script>

<template>
  <div v-if="isLoggedIn" class="floating-chat">
    <!-- 右下角聊天入口 -->
    <button
      v-if="!isOpen"
      type="button"
      class="chat-launcher"
      aria-label="開啟私人訊息"
      @click="openChat"
    >
      <i class="bi bi-chat-dots-fill"></i>
      <span>聊天</span>
      <span
        v-if="hasUnreadMessages"
        class="chat-notification-dot"
        aria-label="有未讀訊息"
      ></span>
    </button>

    <!-- 聊天視窗 -->
    <section
      v-if="isOpen"
      class="chat-window"
      :class="{
        minimized: isMinimized,
        expanded: isExpanded,
      }"
      aria-label="私人聊天室"
    >
      <!-- 標題列 -->
      <header class="chat-header">
        <!-- 返回聊天室列表 -->
        <button
          v-if="activeConversation && !isMinimized"
          type="button"
          class="header-icon back-icon"
          aria-label="返回聊天室列表"
          title="返回列表"
          @click="backToList"
        >
          <i class="bi bi-arrow-left"></i>
        </button>

        <div class="header-title">
          <i class="bi bi-chat-dots-fill"></i>
          <span>
            {{
              activeConversation
                ? activeConversation.otherUserName || '私人聊天室'
                : '私人訊息'
            }}
          </span>
        </div>

        <div class="header-actions">
          <!-- 開啟完整聊天室 -->
          <button
            v-if="activeConversation && !isMinimized"
            type="button"
            class="header-icon detail-icon"
            aria-label="開啟完整聊天室"
            title="開啟完整聊天室"
            @click="openFullConversation"
          >
            <i class="bi bi-chat-square-text"></i>
          </button>

          <!-- 放大／縮小 -->
          <button
            type="button"
            class="header-icon expand-icon"
            :aria-label="isExpanded ? '縮小聊天室' : '放大聊天室'"
            :title="isExpanded ? '縮小聊天室' : '放大聊天室'"
            @click="isExpanded = !isExpanded; isMinimized = false"
          >
            <i
              :class="
                isExpanded
                  ? 'bi bi-arrows-angle-contract'
                  : 'bi bi-arrows-angle-expand'
              "
            ></i>
          </button>

          <!-- 收合 -->
          <button
            type="button"
            class="header-icon close-icon"
            aria-label="收合聊天室"
            title="收合"
            @click="closeChat"
          >
            <i class="bi bi-dash-lg"></i>
          </button>
        </div>
      </header>

      <template v-if="!isMinimized">
        <!-- 聊天室列表 -->
        <div
          v-if="!activeConversation"
          class="conversation-list"
        >
          <div
            v-if="loadingConversations"
            class="chat-state"
          >
            <div class="spinner-border spinner-border-sm"></div>
            <p>載入聊天室中...</p>
          </div>

          <div
            v-else-if="errorMessage"
            class="chat-state"
          >
            <p class="text-danger">{{ errorMessage }}</p>
            <button
              type="button"
              class="btn btn-sm btn-outline-secondary"
              @click="loadConversations"
            >
              再試一次
            </button>
          </div>

          <div
            v-else-if="conversations.length === 0"
            class="chat-state"
          >
            <i class="bi bi-chat-heart empty-icon"></i>
            <p>目前還沒有私人訊息</p>
            <small>前往其他會員的公開頁面開始聊天吧！</small>
          </div>

          <button
            v-for="conversation in conversations"
            :key="conversation.conversationId"
            type="button"
            class="conversation-item"
            @click="openConversation(conversation)"
          >
            <div class="conversation-avatar">
              <i class="bi bi-person-fill"></i>
            </div>

            <div class="conversation-info">
              <div class="conversation-top">
                <strong>
                  {{ conversation.otherUserName || '會員' }}
                </strong>

                <small>
                  {{
                    formatTime(
                      conversation.lastMessageAt ||
                      conversation.updatedAt
                    )
                  }}
                </small>
              </div>

              <div class="conversation-bottom">
                <span>
                  {{
                    conversation.lastMessageContent ||
                    '尚無訊息，開始聊天吧！'
                  }}
                </span>

                <b
                  v-if="conversation.unreadCount > 0"
                  class="unread-badge"
                >
                  {{
                    conversation.unreadCount > 99
                      ? '99+'
                      : conversation.unreadCount
                  }}
                </b>
              </div>
            </div>
          </button>
        </div>

        <!-- 聊天訊息 -->
        <template v-else>
          <div
            v-if="loadingMessages"
            class="chat-state"
          >
            <div class="spinner-border spinner-border-sm"></div>
            <p>載入訊息中...</p>
          </div>

          <div
            v-else-if="errorMessage"
            class="chat-state"
          >
            <p class="text-danger">{{ errorMessage }}</p>
            <button
              type="button"
              class="btn btn-sm btn-outline-secondary"
              @click="openConversation(activeConversation)"
            >
              再試一次
            </button>
          </div>

          <template v-else>
            <div
              ref="messageList"
              class="message-list"
            >
              <div
                v-if="messages.length === 0"
                class="chat-state"
              >
                <i class="bi bi-chat-heart empty-icon"></i>
                <p>還沒有訊息，傳送第一則訊息吧！</p>
              </div>

              <div
                v-for="(message, index) in messages"
                :key="message.messageId ?? index"
                class="message-row"
                :class="{ mine: isMyMessage(message) }"
              >
                <div class="message-bubble">

                  <p>
                    <template v-for="(part, partIndex) in getMessageParts(message.content)" :key="partIndex" >
                    <a
                      v-if="part.url"
                      :href="part.url"
                      target="_blank"
                      rel="noopener noreferrer"
                    >{{ part.text }}</a>
                    <span v-else>{{ part.text }}</span>
                  </template></p>

                  <time>{{ formatTime(message.sentAt) }}</time>
                </div>
              </div>
            </div>

            <form
              class="chat-input"
              @submit.prevent="sendMessage"
            >
              <div
                v-if="sendError"
                class="send-error"
              >
                {{ sendError }}
              </div>

              <textarea
                v-model="messageContent"
                placeholder="輸入訊息..."
                rows="2"
                maxlength="2000"
                :disabled="sending"
                @keydown.enter.exact.prevent="sendMessage"
              ></textarea>

              <button
                type="submit"
                class="send-button"
                :disabled="sending || !messageContent.trim()"
                aria-label="傳送訊息"
                title="傳送訊息"
              >
                <i class="bi bi-send-fill"></i>
              </button>
            </form>
          </template>
        </template>
      </template>
    </section>
  </div>
</template>

<style scoped>
.floating-chat {
  --mint: #a8d5ba;
  --mint-light: #edf6f0;
  --mint-dark: #365844;
  --border: #e0ece3;
  --muted: #84968b;

  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1050;
  color: #405b49;
  font-size: 14px;
}

/* 右下角入口 */
.chat-launcher {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  border: 0;
  border-radius: 999px;
  background: #1e2b45;
  color: #fff;
  box-shadow: 0 6px 20px rgb(0 0 0 / 18%);
  font-weight: 700;
  cursor: pointer;
}

.chat-launcher i {
  color: #ffc857;
  font-size: 1.25rem;
}

.chat-launcher:hover {
  transform: translateY(-2px);
}

/* 聊天視窗 */
.chat-window {
  display: flex;
  flex-direction: column;
  width: 360px;
  height: 480px;
  max-height: calc(100vh - 48px);
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 32px rgb(0 0 0 / 20%);
}

.chat-window.expanded {
  width: min(620px, calc(100vw - 32px));
  height: min(760px, calc(100vh - 48px));
}

.chat-window.minimized {
  height: auto;
}

/* 標題列 */
.chat-header {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 58px;
  padding: 10px 12px;
  background: #1e2b45;
  color: #fff;
}

.header-title {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
  font-weight: 700;
}

.header-title span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-title > i {
  flex-shrink: 0;
  color: #ffc857;
  font-size: 1.1rem;
}

.header-actions {
  display: flex;
  flex-shrink: 0;
  gap: 6px;
}

/*
 * 明確指定背景、圖示顏色及 Bootstrap Icons 字體大小，
 * 避免圖示被深色標題列吃掉對比。
 */
.floating-chat .header-icon {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  color: #1e2b45;
  font-size: 17px;
  line-height: 1;
  cursor: pointer;
}

.floating-chat .header-icon i {
  display: inline-block;
  color: inherit;
  font-size: 17px;
  line-height: 1;
}

.floating-chat .header-icon:hover {
  border-color: #ffc857;
  background: #ffc857;
  color: #1e2b45;
}

.floating-chat .header-icon:focus-visible {
  outline: 3px solid #ffc857;
  outline-offset: 2px;
}

.floating-chat .detail-icon {
  border-color: #a7dfbd;
  background: #d9f3e3;
  color: #205c3a;
}

.floating-chat .detail-icon:hover {
  background: #b7e8c9;
  color: #16472b;
}

.floating-chat .close-icon {
  border-color: #ffcaca;
  background: #ffe5e5;
  color: #a61b1b;
}

.floating-chat .close-icon:hover {
  background: #ffcccc;
  color: #7f1111;
}

/* 聊天室列表 */
.conversation-list {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px;
  border: 0;
  border-bottom: 1px solid #f0f4f1;
  background: #fff;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.conversation-item:hover {
  background: #f7fbf8;
}

.conversation-avatar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--mint-light);
  color: var(--mint-dark);
  font-size: 1.3rem;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-top,
.conversation-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.conversation-top strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-top small {
  flex-shrink: 0;
  color: var(--muted);
  font-size: 11px;
}

.conversation-bottom {
  margin-top: 5px;
  color: var(--muted);
}

.conversation-bottom span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-badge {
  flex-shrink: 0;
  padding: 2px 6px;
  border-radius: 999px;
  background: var(--mint);
  color: var(--mint-dark);
  font-size: 11px;
}

/* 載入、空清單與錯誤畫面 */
.chat-state {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: var(--muted);
  text-align: center;
}

.chat-state p {
  margin: 0;
}

.empty-icon {
  color: var(--mint);
  font-size: 2rem;
}

/* 訊息區 */
.message-list {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  padding: 14px;
}

.message-row {
  display: flex;
  justify-content: flex-start;
}

.message-row.mine {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 80%;
  padding: 10px 13px;
  border-radius: 14px 14px 14px 4px;
  background: var(--mint-light);
  overflow-wrap: anywhere;
}

.message-row.mine .message-bubble {
  border-radius: 14px 14px 4px 14px;
  background: #1e2b45;
  color: #fff;
}

.message-bubble small,
.message-bubble time {
  display: block;
  margin-bottom: 4px;
  color: var(--muted);
  font-size: 11px;
}

.message-row.mine .message-bubble time {
  color: #d2d9e6;
}

.message-bubble p {
  margin: 0;
  white-space: pre-wrap;
}

.chat-input {
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 10px;
  border-top: 1px solid var(--border);
  background: #fff;
}

.chat-input textarea {
  flex: 1;
  min-width: 0;
  resize: none;
  padding: 9px;
  border: 1px solid var(--border);
  border-radius: 10px;
  color: #405b49;
  font: inherit;
}

.chat-input textarea:focus {
  border-color: var(--mint);
  outline: 2px solid var(--mint-light);
}

.send-button {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 10px;
  background: var(--mint);
  color: var(--mint-dark);
  cursor: pointer;
}

.send-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-error {
  position: absolute;
  right: 10px;
  bottom: 100%;
  left: 10px;
  padding: 8px;
  border-radius: 8px;
  background: #fff0f0;
  color: #b42318;
  font-size: 12px;
}

/* 手機版 */
@media (max-width: 480px) {
  .floating-chat {
    right: 12px;
    bottom: 12px;
  }

  .chat-window {
    width: calc(100vw - 24px);
    height: min(560px, calc(100vh - 24px));
  }

  .chat-window.expanded {
    width: calc(100vw - 24px);
    height: calc(100vh - 24px);
  }

  .chat-header {
    gap: 5px;
    padding: 9px;
  }

  .header-actions {
    gap: 4px;
  }

  .floating-chat .header-icon {
    width: 32px;
    height: 32px;
  }
}
.chat-launcher {
  position: relative;
}

.chat-notification-dot {
  position: absolute;
  top: -3px;
  right: -3px;
  width: 13px;
  height: 13px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #ef4444;
  box-shadow: 0 0 0 2px rgb(239 68 68 / 15%);
}
.message-bubble p a {
  color: #1769aa;
  text-decoration: underline;
  overflow-wrap: anywhere;
}

.message-row.mine .message-bubble p a {
  color: #a8d5ff;
}

.message-bubble p a:hover {
  text-decoration-thickness: 2px;
}
</style>