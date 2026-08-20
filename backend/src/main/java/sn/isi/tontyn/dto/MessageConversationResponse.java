package sn.isi.tontyn.dto;

import sn.isi.tontyn.model.MessageConversation;

import java.time.LocalDateTime;

public record MessageConversationResponse(Long id,
                                          String auteur,
                                          String contenu,
                                          LocalDateTime horodatage) {

    public static MessageConversationResponse from(MessageConversation m) {
        return new MessageConversationResponse(m.getId(), m.getAuteur().name(),
                m.getContenu(), m.getHorodatage());
    }
}
