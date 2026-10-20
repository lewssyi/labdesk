package dev.labdesk.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.labdesk.dto.AgentChatRequest;
import dev.labdesk.integration.AgentModelClient;
import dev.labdesk.security.CurrentUser;
import dev.labdesk.service.ReservationQueryService;
import dev.labdesk.vo.ReservationQueries.MyBookings;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentServiceImplTest {
    AgentModelClient model = mock(AgentModelClient.class);
    ReservationQueryService queries = mock(ReservationQueryService.class);
    AgentServiceImpl service = new AgentServiceImpl(model, queries, new ObjectMapper().findAndRegisterModules());
    CurrentUser student = new CurrentUser(1, "student", "STUDENT");
    AgentChatRequest request = new AgentChatRequest("我的预约", null);

    AssistantMessage tool(String name, String args) {
        return AssistantMessage.builder().content("").toolCalls(List.of(
                new AssistantMessage.ToolCall("call_1", "function", name, args))).build();
    }

    @Test void noKeyDoesNotPretendToHaveAnswered() {
        assertEquals("NOT_CONFIGURED", service.chat(student, request).mode());
        verifyNoInteractions(queries);
    }

    @Test void executesToolAndReturnsMatchingToolCallIdToModel() {
        when(model.isConfigured()).thenReturn(true);
        when(queries.myBookings(student, null, null)).thenReturn(new MyBookings(List.of(), false, "本人预约"));
        when(model.chat(anyList(), anyList())).thenReturn(tool("list_my_bookings", "{}"))
                .thenAnswer(invocation -> {
                    List<Message> history = invocation.getArgument(0);
                    var response = (ToolResponseMessage) history.get(history.size() - 1);
                    assertEquals("call_1", response.getResponses().get(0).id());
                    assertTrue(response.getResponses().get(0).responseData().contains("bookings"));
                    return new AssistantMessage("未查到你的预约。");
                });
        var result = service.chat(student, request);
        assertEquals("AGENT", result.mode());
        assertEquals(1, result.tools().size());
        assertTrue(result.tools().get(0).success());
        verify(queries).myBookings(student, null, null);
    }

    @Test void rejectsUnknownToolsWithoutExecutingBusinessCode() {
        when(model.isConfigured()).thenReturn(true);
        when(model.chat(anyList(), anyList())).thenReturn(tool("run_sql", "{}"), new AssistantMessage("无法执行。"));
        var result = service.chat(student, request);
        assertFalse(result.tools().get(0).success());
        verifyNoInteractions(queries);
    }

    @Test void boundsRepeatedToolCallsAndHidesProviderErrorText() {
        when(model.isConfigured()).thenReturn(true);
        when(model.chat(anyList(), anyList())).thenReturn(tool("search_devices", "{\"name\":\"\"}"));
        when(queries.searchDevices("")).thenReturn(List.of());
        assertEquals("LIMIT_REACHED", service.chat(student, request).mode());
        verify(model, times(4)).chat(anyList(), anyList());
        reset(model);
        when(model.isConfigured()).thenReturn(true);
        when(model.chat(anyList(), anyList())).thenThrow(new RuntimeException("secret-provider-response"));
        var result = service.chat(student, request);
        assertEquals("UNAVAILABLE", result.mode());
        assertFalse(result.answer().contains("secret"));
    }

    @Test void concurrentRequestsKeepTheirOwnTrustedIdentity() throws Exception {
        when(model.isConfigured()).thenReturn(true);
        when(model.chat(anyList(), anyList())).thenReturn(tool("list_my_bookings", "{}"));
        when(queries.myBookings(any(), isNull(), isNull())).thenReturn(new MyBookings(List.of(), false, ""));
        var admin = new CurrentUser(2, "admin", "ADMIN");
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> service.chat(student, request));
            var b = executor.submit(() -> service.chat(admin, request));
            a.get(15, TimeUnit.SECONDS); b.get(15, TimeUnit.SECONDS);
            verify(queries, times(3)).myBookings(student, null, null);
            verify(queries, times(3)).myBookings(admin, null, null);
        } finally { executor.shutdownNow(); }
    }
}
