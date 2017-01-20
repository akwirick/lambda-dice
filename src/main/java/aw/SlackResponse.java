package aw;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response to a roll
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlackResponse {
    private String text;
    private String response_type;
}
