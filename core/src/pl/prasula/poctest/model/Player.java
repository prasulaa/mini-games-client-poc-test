package pl.prasula.poctest.model;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.joints.FrictionJoint;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Player {

    private final String id;
    private final Fixture fixture;
    private final FrictionJoint frictionJoint;

}
