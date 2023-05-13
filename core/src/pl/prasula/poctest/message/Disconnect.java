package pl.prasula.poctest.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class Disconnect extends GeneralMsg {
    public static final String NAME = "disconnect";

    private String uid;

}
