<script setup>
    import { ref, computed, onMounted, nextTick } from 'vue'
    import { useRoute, useRouter } from 'vue-router'
    import http from '@/plugins/axios'
    import { useAuth } from '@/composables/useAuth'

    const route = useRoute()
    const router = useRouter()
    const auth = useAuth()

    // 目前聊天室 ID
    const conversationId = computed(() => route.params.conversationId)

    // 目前登入會員 ID
    const currentUserId = computed(() => {
    const user = auth.user.value

    return user?.usersId ?? user?.users_id ?? user?.userId ?? user?.id ?? null
    })

    // 聊天室資料
    const conversation = ref(null)

    // 訊息列表
    const messages = ref([])

    // 輸入的訊息
    const messageContent = ref('')

    // 載入、傳送與錯誤狀態
    const loading = ref(true)
    const sending = ref(false)
    const errorMessage = ref('')
    const sendError = ref('')

    // 訊息列表 DOM
    const messageList = ref(null)

    // 載入聊天室資料與訊息
    async function loadConversation() {
    loading.value = true
    errorMessage.value = ''

    try {
        // 從聊天室列表找出目前聊天室的對象資訊
        const conversationResponse = await http.get('/conversations')

        conversation.value = conversationResponse.data.find(
        item =>
            String(item.conversationId) === String(conversationId.value)
        ) || null

        // 載入訊息
        const response = await http.get(
        `/conversations/${conversationId.value}/messages`,
        {
            params: {
            page: 0,
            size: 100,
            },
        }
        )

        // 依照訊息時間由舊到新排列，最新訊息放在最底部
        messages.value = (response.data.content || []).sort( (a, b) => new Date(a.sentAt) - new Date(b.sentAt) )

        // 將對方傳來的未讀訊息標記為已讀
        await http.patch(
        `/conversations/${conversationId.value}/messages/read`
        )

        await scrollToBottom()
    } catch (error) {
        errorMessage.value =
        error.response?.data?.message ||
        '無法載入聊天室，請稍後再試。'
    } finally {
        loading.value = false
    }
    }

    // 傳送訊息
    async function sendMessage() {
    const content = messageContent.value.trim()

    if (!content || sending.value) {
        return
    }

    if (content.length > 2000) {
        sendError.value = '訊息長度不可超過 2000 字。'
        return
    }

    sending.value = true
    sendError.value = ''

    try {
        const response = await http.post(
        `/conversations/${conversationId.value}/messages`,
        { content }
        )

        // 將後端回傳的訊息加入列表
        messages.value.push(response.data)

        // 清空輸入框
        messageContent.value = ''

        await scrollToBottom()

        // 更新聊天室列表的最後訊息與時間
        await loadConversation()
    } catch (error) {
        sendError.value =
        error.response?.data?.message ||
        '訊息傳送失敗，請稍後再試。'
    } finally {
        sending.value = false
    }
    }

    // 判斷訊息是否由目前登入會員傳送
    function isMyMessage(message) {
    if (currentUserId.value == null || message.senderId == null) {
        return false
    }

    return String(message.senderId) === String(currentUserId.value)
    }

    // 格式化訊息時間
    function formatMessageTime(value) {
    if (!value) return ''

    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return ''
    }

    return date.toLocaleString('zh-TW', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
    })
    }

    // 捲動到最新訊息
    async function scrollToBottom() {
    await nextTick()

    if (messageList.value) {
        messageList.value.scrollTop = messageList.value.scrollHeight
    }
    }

    // 返回訊息列表
    function backToInbox() {
    router.push({ name: 'account-messages' })
    }

    onMounted(loadConversation)
</script>

<template>
    <div class="message-conversation py-4">
        <div class="chat-card">
        <!-- 聊天室標題 -->
        <div class="chat-header">
            <button
            type="button"
            class="btn btn-outline-secondary back-btn"
            @click="backToInbox"
            >
            <i class="bi bi-arrow-left"></i>
            返回
            </button>

            <div class="chat-user">
            <div class="avatar">
                <i class="bi bi-person-fill"></i>
            </div>

            <div>
                <h2 class="chat-title">
                {{ conversation?.otherUserName || '私人聊天室' }}
                </h2>
                <p class="chat-subtitle mb-0">私人訊息</p>
            </div>
            </div>

            <button
            type="button"
            class="btn btn-outline-secondary refresh-btn"
            :disabled="loading"
            @click="loadConversation"
            >
            <i class="bi bi-arrow-clockwise"></i>
            重新載入
            </button>
        </div>

        <!-- 載入中 -->
        <div v-if="loading" class="chat-state">
            <div class="spinner-border" role="status" aria-label="載入中"></div>
            <p class="mt-3 mb-0">正在載入聊天訊息...</p>
        </div>

        <!-- 載入失敗 -->
        <div v-else-if="errorMessage" class="chat-state">
            <p class="text-danger">{{ errorMessage }}</p>
            <button
            type="button"
            class="btn btn-success"
            @click="loadConversation"
            >
            再試一次
            </button>
        </div>

        <!-- 訊息內容 -->
        <template v-else>
            <div ref="messageList" class="chat-messages">
            <div v-if="messages.length === 0" class="no-messages">
                <i class="bi bi-chat-heart"></i>
                <p class="mb-0">還沒有訊息，傳送第一則訊息吧！</p>
            </div>

            <div
                v-for="message in messages"
                :key="message.messageId"
                class="message-row"
                :class="{ 'my-message-row': isMyMessage(message) }"
            >
                <div class="message-bubble-wrap">
                <div
                    v-if="!isMyMessage(message)"
                    class="sender-name"
                >
                    {{ message.senderName || '會員' }}
                </div>

                <div
                    class="message-bubble"
                    :class="{ 'my-message': isMyMessage(message) }"
                >
                    {{ message.content }}
                </div>

                <div class="message-time">
                    {{ formatMessageTime(message.sentAt) }}
                </div>
                </div>
            </div>
            </div>

            <!-- 輸入區 -->
            <form class="chat-input-area" @submit.prevent="sendMessage">
            <div v-if="sendError" class="send-error">
                {{ sendError }}
            </div>

            <div class="chat-input-row">
                <textarea
                v-model="messageContent"
                class="form-control chat-textarea"
                placeholder="輸入訊息..."
                rows="2"
                maxlength="2000"
                :disabled="sending"
                @keydown.enter.exact.prevent="sendMessage"
                ></textarea>

                <button
                type="submit"
                class="btn btn-success send-btn"
                :disabled="sending || !messageContent.trim()"
                >
                <span
                    v-if="sending"
                    class="spinner-border spinner-border-sm"
                    aria-hidden="true"
                ></span>
                <i v-else class="bi bi-send-fill"></i>
                {{ sending ? '傳送中' : '傳送' }}
                </button>
            </div>

            <div class="input-hint">
                Enter 傳送訊息，Shift + Enter 換行（最多 2000 字）
            </div>
            </form>
        </template>
        </div>
    </div>
</template>

<style scoped>
    .message-conversation {
    width: 100%;
    max-width: 1100px;
    min-width: 0;
    margin: 0 auto;
    padding-right: 1.25rem;
    padding-left: 1.25rem;
    color: #405b49;

    --mint: #a8d5ba;
    --mint-light: #edf6f0;
    --mint-dark: #365844;
    --border: #e0ece3;
    --muted: #84968b;
    }

    .chat-card {
    display: flex;
    flex-direction: column;
    width: 100%;
    min-width: 0;
    min-height: 600px;
    overflow: hidden;
    border: 1px solid var(--border);
    border-radius: 1.25rem;
    background: #fff;
    box-shadow: 0 4px 16px rgb(87 130 99 / 8%);
    }

    .chat-header {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 1.25rem 1.5rem;
    border-bottom: 1px solid var(--border);
    }

    .back-btn,
    .refresh-btn {
    flex-shrink: 0;
    border-color: #c8ddcf;
    border-radius: 0.7rem;
    color: #587363;
    }

    .back-btn:hover,
    .refresh-btn:hover:not(:disabled) {
    border-color: #d7e9dc;
    background: var(--mint-light);
    color: var(--mint-dark);
    }

    .chat-user {
    display: flex;
    flex: 1;
    align-items: center;
    gap: 0.75rem;
    min-width: 0;
    }

    .avatar {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: var(--mint-light);
    color: var(--mint-dark);
    font-size: 1.4rem;
    }

    .chat-title {
    overflow: hidden;
    margin: 0;
    color: var(--mint-dark);
    font-size: 1.15rem;
    font-weight: 800;
    text-overflow: ellipsis;
    white-space: nowrap;
    }

    .chat-subtitle {
    margin-top: 0.2rem;
    color: var(--muted);
    font-size: 0.82rem;
    }

    .chat-state {
    flex: 1;
    padding: 4rem 1.5rem;
    color: var(--muted);
    text-align: center;
    }

    .chat-state .spinner-border {
    color: var(--mint);
    }

    .chat-messages {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 1.1rem;
    min-height: 350px;
    max-height: 560px;
    overflow-y: auto;
    padding: 1.5rem;
    background: #fbfdfb;
    }

    .no-messages {
    display: flex;
    flex: 1;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0.75rem;
    padding: 3rem 1rem;
    color: var(--muted);
    text-align: center;
    }

    .no-messages i {
    color: var(--mint);
    font-size: 2.5rem;
    }

    .message-row {
    display: flex;
    justify-content: flex-start;
    width: 100%;
    }

    .my-message-row {
    justify-content: flex-end;
    }

    .message-bubble-wrap {
    max-width: min(75%, 560px);
    min-width: 0;
    }

    .sender-name {
    margin: 0 0 0.3rem 0.2rem;
    color: var(--muted);
    font-size: 0.8rem;
    }

    .message-bubble {
    overflow-wrap: anywhere;
    padding: 0.75rem 1rem;
    border: 1px solid var(--border);
    border-radius: 1rem 1rem 1rem 0.25rem;
    background: #fff;
    color: #405b49;
    line-height: 1.6;
    white-space: pre-wrap;
    }

    .my-message {
    border-color: var(--mint);
    border-radius: 1rem 1rem 0.25rem 1rem;
    background: var(--mint-light);
    }

    .message-time {
    margin-top: 0.3rem;
    color: var(--muted);
    font-size: 0.72rem;
    }

    .my-message-row .message-time {
    text-align: right;
    }

    .chat-input-area {
    padding: 1rem 1.25rem 0.8rem;
    border-top: 1px solid var(--border);
    background: #fff;
    }

    .chat-input-row {
    display: flex;
    align-items: flex-end;
    gap: 0.75rem;
    }

    .chat-textarea {
    min-width: 0;
    resize: vertical;
    border-color: #d7e5da;
    border-radius: 0.8rem;
    box-shadow: none;
    }

    .chat-textarea:focus {
    border-color: var(--mint);
    box-shadow: 0 0 0 0.2rem rgb(168 213 186 / 25%);
    }

    .send-btn {
    display: inline-flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    gap: 0.4rem;
    min-height: 44px;
    border-color: var(--mint);
    border-radius: 0.8rem;
    background: var(--mint);
    color: #294b39;
    }

    .send-btn:hover:not(:disabled) {
    border-color: #8bc5a2;
    background: #8bc5a2;
    color: #203d2e;
    }

    .send-btn:disabled {
    opacity: 0.6;
    }

    .send-error {
    margin-bottom: 0.6rem;
    color: #dc3545;
    font-size: 0.875rem;
    }

    .input-hint {
    margin-top: 0.45rem;
    color: var(--muted);
    font-size: 0.75rem;
    text-align: right;
    }

    @media (max-width: 575.98px) {
    .chat-card {
        min-height: 520px;
    }

    .chat-header {
        flex-wrap: wrap;
        gap: 0.75rem;
        padding: 1rem;
    }

    .chat-user {
        order: -1;
        width: 100%;
        flex-basis: 100%;
    }

    .chat-messages {
        min-height: 300px;
        padding: 1rem;
    }

    .message-bubble-wrap {
        max-width: 88%;
    }

    .chat-input-area {
        padding: 0.85rem;
    }
    }
</style>
