package pl.prasula.poctest.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Move extends GeneralMsg {

    public static final String NAME = "move";

    private float x;
    private float y;

}
