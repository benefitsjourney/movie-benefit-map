// ========== src/composables/useExchangeActions.js ==========
// 管理交換／票券表單、新增修改、Modal、收藏及刊登狀態操作。
import { reactive, ref, watch } from "vue";
import Swal from "sweetalert2";

import {
    createExchangeListing,
    updateExchangeListing,
    updateExchangeListingStatus,
    addExchangeFavorite,
    removeExchangeFavorite,
    createTicketListing,
    updateTicketListing,
    updateTicketListingStatus,
    addTicketFavorite,
    removeTicketFavorite,
} from "@/api/exchangeApi.js";

export function useExchangeActions({
    token,
    isLoggedIn,
    goToLogin,
    data,
}) {
    const {
        listingId,
        firstValue,
        toDateInputValue,
        toLocalDateTime,
        statusLabel,
        errorText,
        isFavorited,
        loadPublicListings,
        loadMemberData,
    } = data;

    // ========== 編輯中的刊登 ID ==========
    const editingExchangeId = ref(null);
    const editingTicketId = ref(null);

    // ========== 交換、票券表單彈窗狀態 ==========
    const isExchangeModalOpen = ref(false);
    const isSavingExchange = ref(false);
    const isTicketModalOpen = ref(false);
    const isSavingTicket = ref(false);

    // ========== 交換表單 ==========
    const exchangeForm = reactive({
        title: "",
        description: "",
        useBenefitId: "",
    });

    // ========== 票券表單 ==========
    const ticketForm = reactive({
        title: "",
        description: "",
        cinemaId: "",
        movieId: "",
        ticketShowtime: "",
    });

    // ========== 重設交換表單 ==========
    function resetExchangeForm() {
        editingExchangeId.value = null;

        exchangeForm.title = "";
        exchangeForm.description = "";
        exchangeForm.useBenefitId = "";
    }

    // ========== 開啟新增交換刊登彈窗 ==========
    function openCreateExchangeModal() {
        // 新增模式：清空表單與編輯 ID
        resetExchangeForm();

        // 開啟彈窗
        isExchangeModalOpen.value = true;
    }

    // ========== 關閉交換刊登彈窗 ==========
    function closeExchangeModal() {
        // 儲存期間不允許手動關閉
        if (isSavingExchange.value) {
            return;
        }

        isExchangeModalOpen.value = false;

        // 清除未儲存的表單內容
        resetExchangeForm();
    }

    // ========== 彈窗被關閉時，清理交換表單 ==========
    watch(isExchangeModalOpen, (isOpen) => {
        if (!isOpen && !isSavingExchange.value) {
            resetExchangeForm();
        }
    });

    // ========== 重設票券表單 ==========
    function resetTicketForm() {
        editingTicketId.value = null;

        ticketForm.title = "";
        ticketForm.description = "";
        ticketForm.cinemaId = "";
        ticketForm.movieId = "";
        ticketForm.ticketShowtime = "";
    }

    // ========== 開啟修改交換刊登彈窗 ==========
    function beginEditExchange(item) {
        editingExchangeId.value =
            listingId(item, "exchange");

        exchangeForm.title = item?.title ?? "";
        exchangeForm.description = item?.description ?? "";

        exchangeForm.useBenefitId = String(
            firstValue(
                item,
                ["useBenefitId", "use_benefit_id"],
                "",
            ),
        );

        // 不切換會員頁籤，直接在原畫面開啟彈窗
        isExchangeModalOpen.value = true;
    }

    // ========== 開啟修改票券刊登彈窗 ==========
    function beginEditTicket(item) {
        editingTicketId.value =
            listingId(item, "ticket");

        ticketForm.title = item?.title ?? "";
        ticketForm.description = item?.description ?? "";

        ticketForm.cinemaId = String(
            firstValue(
                item,
                ["cinemaId", "cinema_id"],
                "",
            ),
        );

        ticketForm.movieId = String(
            firstValue(
                item,
                ["movieId", "movie_id"],
                "",
            ),
        );

        ticketForm.ticketShowtime = toDateInputValue(
            firstValue(
                item,
                ["ticketShowtime", "ticket_showtime"],
                "",
            ),
        );

        isTicketModalOpen.value = true;
    }

    // ========== 新增／修改交換刊登 ==========
    async function submitExchangeForm() {
        if (isSavingExchange.value) {
            return;
        }

        if (!token.value) {
            await Swal.fire(
                "請先登入",
                "新增或修改刊登需要有效的登入 Token。",
                "warning",
            );
            return;
        }

        if (!exchangeForm.title.trim()) {
            await Swal.fire(
                "請輸入標題",
                "交換刊登標題不可空白。",
                "warning",
            );
            return;
        }

        if (!exchangeForm.useBenefitId) {
            await Swal.fire(
                "請選擇持有特典",
                "請從自己的持有特典中選擇一筆資料。",
                "warning",
            );
            return;
        }

        const isEditing =
            editingExchangeId.value !== null;

        const payload = {
            title: exchangeForm.title.trim(),
            description: exchangeForm.description.trim(),
            useBenefitId: Number(exchangeForm.useBenefitId),
        };

        isSavingExchange.value = true;

        try {
            if (isEditing) {
                await updateExchangeListing(
                    editingExchangeId.value,
                    payload,
                    token.value,
                );
            } else {
                await createExchangeListing(
                    payload,
                    token.value,
                );
            }

            // 儲存成功：先關閉彈窗並清除表單
            resetExchangeForm();
            isExchangeModalOpen.value = false;

            await Swal.fire(
                "完成",
                isEditing
                    ? "交換刊登修改成功。"
                    : "交換刊登新增成功。",
                "success",
            );

            // 重新載入公開刊登及會員資料
            await loadPublicListings();
            await loadMemberData();
        } catch (error) {
            // 儲存失敗時不關閉彈窗，保留使用者輸入內容
            await Swal.fire(
                "操作失敗",
                errorText(error),
                "error",
            );
        } finally {
            isSavingExchange.value = false;
        }
    }

    // ========== 新增／修改票券刊登 ==========
    async function submitTicketForm() {
        if (isSavingTicket.value) return;

        if (!token.value) {
            await Swal.fire(
                "請先登入",
                "新增或修改票券刊登需要有效的登入 Token。",
                "warning",
            );
            return;
        }

        if (!ticketForm.title.trim()) {
            await Swal.fire(
                "請輸入標題",
                "票券刊登標題不可空白。",
                "warning",
            );
            return;
        }

        if (
            !ticketForm.cinemaId ||
            !ticketForm.movieId ||
            !ticketForm.ticketShowtime
        ) {
            await Swal.fire(
                "資料尚未完整",
                "請選擇影城、電影及放映日期。",
                "warning",
            );
            return;
        }

        const isEditing =
            editingTicketId.value !== null;

        isSavingTicket.value = true;

        try {
            const payload = {
                title: ticketForm.title.trim(),
                description: ticketForm.description.trim(),
                cinemaId: Number(ticketForm.cinemaId),
                movieId: Number(ticketForm.movieId),
                ticketShowtime: toLocalDateTime(
                    ticketForm.ticketShowtime,
                ),
            };

            if (isEditing) {
                await updateTicketListing(
                    editingTicketId.value,
                    payload,
                    token.value,
                );
            } else {
                await createTicketListing(
                    payload,
                    token.value,
                );
            }

            isTicketModalOpen.value = false;
            resetTicketForm();

            await Swal.fire(
                "完成",
                isEditing
                    ? "票券刊登修改成功。"
                    : "票券刊登新增成功。",
                "success",
            );

            await loadPublicListings();
            await loadMemberData();
        } catch (error) {
            await Swal.fire(
                "操作失敗",
                errorText(error),
                "error",
            );
        } finally {
            isSavingTicket.value = false;
        }
    }

    // ========== 關閉票券彈窗 ==========
    function closeTicketModal() {
        if (isSavingTicket.value) return;

        isTicketModalOpen.value = false;
        resetTicketForm();
    }

    // ========== 修改刊登狀態 ==========
    async function changeStatus(item, kind, status) {
        if (!token.value) return;

        const id = listingId(item, kind);

        if (!id) {
            await Swal.fire(
                "無法修改",
                "找不到這筆刊登的 ID。",
                "error",
            );
            return;
        }

        const confirmed = await Swal.fire({
            title: "確定要變更刊登狀態嗎？",
            text: `新狀態：${statusLabel(status)}`,
            icon: "question",
            showCancelButton: true,
            confirmButtonText: "確定",
            cancelButtonText: "取消",
        });

        if (!confirmed.isConfirmed) {
            return;
        }

        try {
            if (kind === "exchange") {
                await updateExchangeListingStatus(
                    id,
                    status,
                    token.value,
                );
            } else {
                await updateTicketListingStatus(
                    id,
                    status,
                    token.value,
                );
            }

            await loadPublicListings();
            await loadMemberData();

            await Swal.fire(
                "完成",
                "刊登狀態已更新。",
                "success",
            );
        } catch (error) {
            await Swal.fire(
                "修改失敗",
                errorText(error),
                "error",
            );
        }
    }

    // ========== 加入收藏／取消收藏 ==========
    async function toggleFavorite(item, kind) {
        if (!isLoggedIn.value || !token.value) {
            await goToLogin();
            return;
        }

        const id = listingId(item, kind);

        if (!id) {
            await Swal.fire(
                "無法收藏",
                "找不到刊登 ID，請確認後端回傳資料。",
                "error",
            );
            return;
        }

        const favorited = isFavorited(id, kind);

        try {
            if (kind === "exchange") {
                if (favorited) {
                    await removeExchangeFavorite(
                        id,
                        token.value,
                    );
                } else {
                    await addExchangeFavorite(
                        id,
                        token.value,
                    );
                }
            } else {
                if (favorited) {
                    await removeTicketFavorite(
                        id,
                        token.value,
                    );
                } else {
                    await addTicketFavorite(
                        id,
                        token.value,
                    );
                }
            }

            await loadMemberData();
        } catch (error) {
            await Swal.fire(
                "收藏操作失敗",
                errorText(error),
                "error",
            );
        }
    }

    return {
        editingExchangeId,
        editingTicketId,

        isExchangeModalOpen,
        isSavingExchange,
        isTicketModalOpen,
        isSavingTicket,

        exchangeForm,
        ticketForm,

        resetExchangeForm,
        resetTicketForm,
        openCreateExchangeModal,
        closeExchangeModal,
        closeTicketModal,
        beginEditExchange,
        beginEditTicket,

        submitExchangeForm,
        submitTicketForm,
        changeStatus,
        toggleFavorite,
    };
}