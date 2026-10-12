
package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.TicketFavoriteBean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 用於回傳票券收藏資料給前端。
public class TicketFavoriteResponse {

    private Integer ticketFavoriteId;
    private Integer ticketListingId;
    private LocalDateTime createdAt;

    public static TicketFavoriteResponse from(TicketFavoriteBean bean) {

        TicketFavoriteResponse response = new TicketFavoriteResponse();

        response.setTicketFavoriteId(bean.getTicketFavoriteId());

        if (bean.getTicketListingBean() != null) {
            response.setTicketListingId(bean.getTicketListingBean().getTicketListingId());
        }

        response.setCreatedAt(bean.getCreatedAt());

        return response;
    }
}