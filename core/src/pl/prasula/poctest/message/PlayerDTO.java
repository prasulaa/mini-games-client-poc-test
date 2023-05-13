package pl.prasula.poctest.message;

import com.badlogic.gdx.math.Vector2;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.prasula.poctest.model.Player;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
public class PlayerDTO extends GeneralMsg {

    public static final String NAME = "player";

    private String id;
    private float x;
    private float y;

    public static PlayerDTO fromPlayer(Player player) {
        Vector2 pos = player.getFixture().getBody().getPosition();
        return new PlayerDTO(player.getId(), pos.x, pos.y);
    }

}