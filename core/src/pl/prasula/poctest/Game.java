package pl.prasula.poctest;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.FrictionJoint;
import com.badlogic.gdx.physics.box2d.joints.FrictionJointDef;
import com.badlogic.gdx.utils.ScreenUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import pl.prasula.poctest.message.GeneralMsg;
import pl.prasula.poctest.message.Move;
import pl.prasula.poctest.message.PlayerDTO;
import pl.prasula.poctest.message.WorldInfo;
import pl.prasula.poctest.model.Player;

import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

// TODO
// udp!!!

public class Game extends ApplicationAdapter {

	private static final int WIDTH = 800;
	private static final int HEIGHT = 480;
	private static final int PPM = 64;
	private static final float IMPULSE = 1f;
	private static final float MAX_VELOCITY = 0.5f;
	private static final int TICK_RATE = 20;
	private static final String SERVER_ID = "f3cb90a4351f44a79f1c9aca0b1f4869";
	private static final float SYNC_ERR = MAX_VELOCITY;

	private SpriteBatch batch;
	private OrthographicCamera camera;

	private World world;
	private Box2DDebugRenderer debugRenderer;

	private Map<String, Player> otherPlayers;
	private Player player;

	private WebSocketClient webSocketClient;
	private ObjectMapper objectMapper;
	private Fixture ground;

	@Override
	public void create () {
		objectMapper = new ObjectMapper();
		URI uri = null;
		try {
			uri = new URI("ws://localhost:8080/servers/" + SERVER_ID);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		webSocketClient = new WebSocketClient(uri) {
			private final Timer timer = new Timer(true);
			@Override
			public void onOpen(ServerHandshake handshake) {

			}

			@Override
			public void onMessage(String message) {
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						Gdx.app.postRunnable(() -> {
							handleMsg(message);
						});
					}
				}, 100);

			}

			@Override
			public void onClose(int code, String reason, boolean remote) {

			}

			@Override
			public void onError(Exception ex) {
				ex.printStackTrace();

				if (ex instanceof ConnectException) {
					Gdx.app.exit();
				}
			}
		};
		webSocketClient.connect();

		batch = new SpriteBatch();
		camera = new OrthographicCamera();
		camera.setToOrtho(false, WIDTH, HEIGHT);

		world = new World(new Vector2(0, 0), true);
		debugRenderer = new Box2DDebugRenderer();
		otherPlayers = new HashMap<>();

		BodyDef groundBodyDef = new BodyDef();
		groundBodyDef.position.set(new Vector2(5, 5));
		groundBodyDef.type = BodyDef.BodyType.StaticBody;

		Body groundBody = world.createBody(groundBodyDef);

		PolygonShape groundBox = new PolygonShape();
		groundBox.setAsBox(50, 50);
		FixtureDef groundFixtureDef = new FixtureDef();
		groundFixtureDef.shape = groundBox;
		groundFixtureDef.density = 0.0f;
		groundFixtureDef.isSensor = true;

		ground = groundBody.createFixture(groundFixtureDef);
		groundBox.dispose();
	}

	private void handleMsg(String message) {
		try {
			GeneralMsg msg = objectMapper.readValue(message, GeneralMsg.class);

			if (msg.getMsgType().equals(PlayerDTO.NAME)) {
				PlayerDTO player = (PlayerDTO) msg;
				this.player = createPlayer(player);
			} else if (msg.getMsgType().equals(WorldInfo.NAME)) {
				WorldInfo info = (WorldInfo) msg;

				for (PlayerDTO p: info.getPlayers()) {
					Player other;
					if ((other = otherPlayers.get(p.getId())) != null) { // update present (in list) bodies
						Body body = other.getFixture().getBody();
						Vector2 position = body.getPosition();

						float xDiff = p.getX() - position.x;
						float yDiff = p.getY() - position.y;

						if (Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2)) > SYNC_ERR) {
							body.setTransform(p.getX(), p.getY(), 0f);
						} else {
							body.setLinearVelocity(xDiff * TICK_RATE, yDiff * TICK_RATE);
						}
					} else if (p.getId().equals(player.getId())) { // update main player
						Body body = player.getFixture().getBody();
						Vector2 playerPos = body.getPosition();

						float xDiff = p.getX() - playerPos.x;
						float yDiff = p.getY() - playerPos.y;

						if (Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2)) > SYNC_ERR) {
							body.setTransform(p.getX(), p.getY(), 0f);
						}
					} else { // add not present (in list) bodies
						Player newPlayer = createPlayer(p);
						otherPlayers.put(newPlayer.getId(), newPlayer);
					}
				}

				otherPlayers.entrySet().stream() // delete present (in list) and not present (in msg) bodies
						.filter(entry -> info.getPlayers().stream().noneMatch((p) -> p.getId().equals(entry.getKey())))
						.collect(Collectors.toList())
						.forEach(entry -> {
							world.destroyJoint(entry.getValue().getFrictionJoint());
							world.destroyBody(entry.getValue().getFixture().getBody());
							otherPlayers.remove(entry.getKey());
						});
			}
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void render () {
		// GRAPHICS
		camera.update();
		ScreenUtils.clear(0, 0, 0, 1);
		batch.setProjectionMatrix(camera.combined);
		batch.begin();



		batch.end();
		debugRenderer.render(world, camera.combined.scl(PPM));

		if (player != null && webSocketClient.isOpen()) {
			sendPlayerInfoToServer();
		}

//		if (player != null) {
//			Vector2 vel = player.getFixture().getBody().getLinearVelocity();
//			System.out.println(Math.sqrt(vel.x * vel.x + vel.y * vel.y));
//		}

		doPhysicsStep(Gdx.graphics.getDeltaTime());
	}

	@Override
	public void dispose () {
		batch.dispose();
		//TODO DISPOSE OTHERS
		webSocketClient.close();
	}

	public void doPhysicsStep(float deltaTime) {
		float dt = 1/60f;

		while (deltaTime > 0.0) {
			float timeStep = Math.min(deltaTime, dt);
			world.step(timeStep, 6, 2);
			deltaTime -= timeStep;
		}
	}

	private void sendPlayerInfoToServer() {
		float x = 0, y = 0;
		Body body = player.getFixture().getBody();
		Vector2 velocity = body.getLinearVelocity();
		Vector2 pos = body.getPosition();

		if (Gdx.input.isKeyPressed(Input.Keys.A)) {
			x += -1f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.D)) {
			x += 1f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.W)) {
			y += 1f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.S)) {
			y += -1f;
		}

		Vector2 mov = new Vector2(x, y).nor();
		body.setLinearVelocity(mov.x*IMPULSE, mov.y*IMPULSE);

		try {
			webSocketClient.send(objectMapper.writeValueAsString(new Move(mov.x, mov.y)));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private Player createPlayer(PlayerDTO player) {
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyDef.BodyType.DynamicBody;
		bodyDef.position.set(player.getX(), player.getY());
		bodyDef.fixedRotation = true;

		Body body = world.createBody(bodyDef);
		CircleShape shape = new CircleShape();
		shape.setRadius(0.5f);
		Fixture fixture = body.createFixture(shape, 1f);
		shape.dispose();

		FrictionJointDef jointDef = new FrictionJointDef();
		jointDef.maxForce = 1f;
		jointDef.maxTorque = 1f;
		jointDef.initialize(body, ground.getBody(), new Vector2(0, 0));
		FrictionJoint frictionJoint = (FrictionJoint)world.createJoint(jointDef);

		return new Player(player.getId(), fixture, frictionJoint);
	}

}
