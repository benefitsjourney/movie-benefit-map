<script setup>
    import { ref, onMounted } from 'vue'
    import { useRouter } from 'vue-router'
    import http from '@/plugins/axios'

    // 路由
    const router = useRouter()

    // 聊天室列表
    const conversations = ref([])

    // 載入狀態
    const loading = ref(true)
    const errorMessage = ref('')

    // 取得目前會員的聊天室列表
    async function loadConversations() {
    loading.value = true
    errorMessage.value = ''

    try {
        const response = await http.get('/conversations')
        conversations.value = response.data
    } catch (error) {
        errorMessage.value =
        error.response?.data?.message ||
        '無法載入訊息列表，請稍後再試。'
    } finally {
        loading.value = false
    }
    }

    // 開啟聊天室
    function openConversation(conversation) {
    router.push({
        name: 'message-conversation',
        params: {
        conversationId: conversation.conversationId,
        },
    })
    }

    // 顯示最後訊息
    function getLastMessage(conversation) {
    return conversation.lastMessageContent || '尚無訊息，開始聊天吧！'
    }

    // 格式化時間
    function formatTime(value) {
    if (!value) return ''

    const date = new Date(value)

    if (Number.isNaN(date.getTime())) return ''

    const now = new Date()
    const isToday =
        date.getFullYear() === now.getFullYear() &&
        date.getMonth() === now.getMonth() &&
        date.getDate() === now.getDate()

    if (isToday) {
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

    onMounted(loadConversations)
</script>

    <template>
    <div class="message-inbox py-4">
        <div class="inbox-card">
        <!-- 標題 -->
        <div class="inbox-header">
            <div>
            <h2 class="mb-1">我的訊息</h2>
            <p class="mb-0">查看你與其他會員的私人對話</p>
            </div>

            <button
            type="button"
            class="btn btn-outline-secondary refresh-btn"
            :disabled="loading"
            @click="loadConversations"
            >
            {{ loading ? '載入中...' : '重新載入' }}
            </button>
        </div>

        <!-- 載入中 -->
        <div v-if="loading" class="empty-state">
            <div
            class="spinner-border"
            role="status"
            aria-label="載入中"
            ></div>
            <p class="mt-3 mb-0">正在載入訊息...</p>
        </div>

        <!-- 載入失敗 -->
        <div v-else-if="errorMessage" class="empty-state">
            <p class="text-danger mb-3">{{ errorMessage }}</p>
            <button
            type="button"
            class="btn btn-success"
            @click="loadConversations"
            >
            再試一次
            </button>
        </div>

        <!-- 尚無聊天室 -->
        <div
            v-else-if="conversations.length === 0"
            class="empty-state"
        >
            <div class="empty-icon">
            <i class="bi bi-chat-dots"></i>
            </div>
            <h5>目前還沒有私人訊息</h5>
            <p class="mb-0">
            前往其他會員的公開頁面，即可開始聊天。
            </p>
        </div>

        <!-- 聊天室列表 -->
        <div v-else class="conversation-list">
          <button  v-for="conversation in conversations" :key="conversation.conversationId" type="button"
            class="conversation-item" @click="openConversation(conversation)" >
            <div class="avatar">
                <i class="bi bi-person-fill"></i>
            </div>

            <div class="conversation-content">
                <div class="conversation-top">
                <h6 class="conversation-name">
                    {{ conversation.otherUserName || '會員' }}
                </h6>

                <span class="conversation-time">
                    {{
                    formatTime(
                        conversation.lastMessageAt ||
                        conversation.updatedAt
                    )
                    }}
                </span>
                </div>

                <div class="conversation-bottom">
                    <p class="last-message"> {{ getLastMessage(conversation) }}  </p>

                    <span  v-if="conversation.unreadCount > 0" class="unread-badge" >
                        {{ conversation.unreadCount > 99  ? '99+' : conversation.unreadCount }} </span>
                </div>
            </div>
          </button>
            </div>
        </div>
    </div>
</template>

<style scoped>
    .message-inbox {
    width: 100%;
    min-width: 0;

    --mint: #a8d5ba;
    --mint-light: #edf6f0;
    --mint-dark: #365844;
    --border: #e0ece3;
    --text: #405b49;
    --muted: #84968b;

    color: var(--text);
    }

    .inbox-card {
    overflow: hidden;
    width: 100%;
    border: 1px solid var(--border);
    border-radius: 1.25rem;
    background: #fff;
    box-shadow: 0 4px 16px rgb(87 130 99 / 8%);
    }

    .inbox-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    padding: 1.5rem;
    border-bottom: 1px solid var(--border);
    }

    .inbox-header h2 {
    color: var(--mint-dark);
    font-size: 1.5rem;
    font-weight: 800;
    }

    .inbox-header p {
    color: var(--muted);
    font-size: 0.9rem;
    }

    .refresh-btn {
    flex-shrink: 0;
    border-color: #c8ddcf;
    border-radius: 0.7rem;
    color: #587363;
    }

    .refresh-btn:hover:not(:disabled) {
    border-color: #d7e9dc;
    background: var(--mint-light);
    color: var(--mint-dark);
    }

    .empty-state {
    padding: 4rem 1.5rem;
    color: var(--muted);
    text-align: center;
    }

    .empty-state .spinner-border {
    color: var(--mint);
    }

    .empty-state h5 {
    margin-top: 1rem;
    color: var(--text);
    font-weight: 700;
    }

    .empty-icon {
    color: var(--mint);
    font-size: 3rem;
    }

    .btn-success {
    border-color: var(--mint);
    background: var(--mint);
    color: #294b39;
    }

    .btn-success:hover {
    border-color: #8bc5a2;
    background: #8bc5a2;
    color: #203d2e;
    }

    .conversation-list {
    display: flex;
    flex-direction: column;
    }

    .conversation-item {
    display: flex;
    align-items: center;
    gap: 1rem;
    width: 100%;
    padding: 1rem 1.5rem;
    border: 0;
    border-bottom: 1px solid #f0f4f1;
    background: #fff;
    color: inherit;
    text-align: left;
    transition: background-color 0.2s ease;
    }

    .conversation-item:last-child {
    border-bottom: 0;
    }

    .conversation-item:hover {
    background: #f7fbf8;
    }

    .avatar {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: var(--mint-light);
    color: var(--mint-dark);
    font-size: 1.5rem;
    }

    .conversation-content {
    flex: 1;
    min-width: 0;
    }

    .conversation-top,
    .conversation-bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;
    }

    .conversation-name {
    overflow: hidden;
    margin: 0;
    color: var(--text);
    font-weight: 700;
    text-overflow: ellipsis;
    white-space: nowrap;
    }

    .conversation-time {
    flex-shrink: 0;
    color: var(--muted);
    font-size: 0.78rem;
    }

    .last-message {
    overflow: hidden;
    margin: 0.35rem 0 0;
    color: #7b8c81;
    font-size: 0.9rem;
    text-overflow: ellipsis;
    white-space: nowrap;
    }

    .unread-badge {
    display: inline-flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    min-width: 22px;
    height: 22px;
    padding: 0 6px;
    border-radius: 999px;
    background: var(--mint);
    color: #294b39;
    font-size: 0.75rem;
    font-weight: 700;
    }

    @media (max-width: 575.98px) {
    .inbox-header {
        align-items: flex-start;
        padding: 1.25rem;
    }

    .inbox-header h2 {
        font-size: 1.3rem;
    }

    .conversation-item {
        gap: 0.75rem;
        padding: 1rem;
    }

    .avatar {
        width: 42px;
        height: 42px;
    }
    }
</style>
