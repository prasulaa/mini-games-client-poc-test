package pl.prasula.poctest.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "msgType",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ConnectionRequest.class, name = ConnectionRequest.NAME),
        @JsonSubTypes.Type(value = Disconnect.class, name = Disconnect.NAME),
        @JsonSubTypes.Type(value = WorldInfo.class, name = WorldInfo.NAME),
        @JsonSubTypes.Type(value = PlayerDTO.class, name = PlayerDTO.NAME),
        @JsonSubTypes.Type(value = Move.class, name = Move.NAME)
})
@NoArgsConstructor
@Setter
@Getter
public class GeneralMsg {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String msgType;
    protected long timestamp = System.currentTimeMillis();

}
