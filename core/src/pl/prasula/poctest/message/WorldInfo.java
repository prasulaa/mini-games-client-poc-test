package pl.prasula.poctest.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
public class WorldInfo extends GeneralMsg {
    public static final String NAME = "worldInfo";

    private List<PlayerDTO> players;

}
