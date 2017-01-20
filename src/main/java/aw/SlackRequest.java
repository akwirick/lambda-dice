package aw;

import lombok.Data;

/**
 * Incoming request from slack
 */
@Data
public class SlackRequest {
    private String token;
    private String user_name;
    private String command;
    private String channel_name;
    private String text;

}
