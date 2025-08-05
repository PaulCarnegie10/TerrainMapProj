package flighttracker;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.*;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.stage.Stage;
import javafx.scene.transform.Rotate;
import javafx.animation.*;
import javafx.util.*;
import com.fazecast.jSerialComm.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * JavaFX application that displays a 3D cube and allows the user to fly the camera around using keyboard controls.
 * - WASD: Move camera forward/left/back/right
 * - Q/E: Move camera up/down
 * - Arrow keys: Look around (rotate camera)
 */
public class Main extends Application {

    // Camera movement parameters
    private static final double CAMERA_MOVE_DELTA = 10;
    private static final double CAMERA_ROTATE_DELTA = 1;

    // Camera state
    private double cameraYaw = 0;   // Y-axis rotation (left/right)
    private double cameraPitch = 0; // X-axis rotation (up/down)

    private static final String SERIAL_PORT = "COM3";
    private static final int BAUD_RATE = 115200;

    private Box cube;
    private double cubeVelX = 0, cubeVelY = 0, cubeVelZ = 0;


    @Override
    public void start(Stage stage) {
        // Create a 3D cube
        Box cube = new Box(100, 100, 100);
        PhongMaterial material = new PhongMaterial(Color.DODGERBLUE);
        cube.setMaterial(material);

        // Center the cube at the origin
        cube.setTranslateX(0);
        cube.setTranslateY(0);
        cube.setTranslateZ(0);

        // Group for 3D objects
        Group root3D = new Group(cube);

        // Camera setup
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1); //How close camera can see
        camera.setFarClip(10000.0); //How far camera can see
        camera.setTranslateZ(-400); // Start back ==> the cube is visible

        // Wrap camera in a camera holder for easier rotation
        Group cameraHolder = new Group();
        cameraHolder.getChildren().add(camera);

        // Root node for the scene
        Group root = new Group(root3D, cameraHolder);

        Scene scene = new Scene(root, 800, 600, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.LIGHTGRAY);
        scene.setCamera(camera);

        // Keyboard controls for camera movement and rotation
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            // Move camera
            if (code == KeyCode.W) {
                moveCamera(camera, cameraHolder, 0, 0, CAMERA_MOVE_DELTA);
            } else if (code == KeyCode.S) {
                moveCamera(camera, cameraHolder, 0, 0, -CAMERA_MOVE_DELTA);
            } else if (code == KeyCode.A) {
                moveCamera(camera, cameraHolder, -CAMERA_MOVE_DELTA, 0, 0);
            } else if (code == KeyCode.D) {
                moveCamera(camera, cameraHolder, CAMERA_MOVE_DELTA, 0, 0);
            } else if (code == KeyCode.Q) {
                moveCamera(camera, cameraHolder, 0, -CAMERA_MOVE_DELTA, 0);
            } else if (code == KeyCode.E) {
                moveCamera(camera, cameraHolder, 0, CAMERA_MOVE_DELTA, 0);
            }
            // Rotate camera
            else if (code == KeyCode.UP) {
                cameraPitch += CAMERA_ROTATE_DELTA;
                camera.setRotationAxis(Rotate.X_AXIS);
                camera.setRotate(cameraPitch);
            } else if (code == KeyCode.DOWN) {
                cameraPitch -= CAMERA_ROTATE_DELTA;
                camera.setRotationAxis(Rotate.X_AXIS);
                camera.setRotate(cameraPitch);
            } else if (code == KeyCode.LEFT) {
                cameraYaw -= CAMERA_ROTATE_DELTA;
                cameraHolder.setRotationAxis(Rotate.Y_AXIS);
                cameraHolder.setRotate(cameraYaw);
            } else if (code == KeyCode.RIGHT) {
                cameraYaw += CAMERA_ROTATE_DELTA;
                cameraHolder.setRotationAxis(Rotate.Y_AXIS);
                cameraHolder.setRotate(cameraYaw);
            }
        });

        stage.setTitle("JavaFX 3D Cube with Flyable Camera");
        stage.setScene(scene);
        stage.show();

        new Thread(this::readSerialData).start();
    }

    private void readSerialData() {
        SerialPort comPort = SerialPort.getCommPort(SERIAL_PORT);
        comPort.setBaudRate(BAUD_RATE);
        if (!comPort.openPort()) {
            System.err.println("Failed to open serial port: " + SERIAL_PORT);
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(comPort.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Example line: ACC:0.12,0.01,9.81;GYRO:0.01,0.02,0.03
                double[] accel = new double[3];
                double[] gyro = new double[3];
                try {
                    String[] parts = line.split(";");
                    for (String part : parts) {
                        if (part.startsWith("ACC:")) {
                            String[] vals = part.substring(4).split(",");
                            accel[0] = Double.parseDouble(vals[0]);
                            accel[1] = Double.parseDouble(vals[1]);
                            accel[2] = Double.parseDouble(vals[2]);
                        } else if (part.startsWith("GYRO:")) {
                            String[] vals = part.substring(5).split(",");
                            gyro[0] = Double.parseDouble(vals[0]);
                            gyro[1] = Double.parseDouble(vals[1]);
                            gyro[2] = Double.parseDouble(vals[2]);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Parse error: " + line);
                    continue;
                }

                // Update cube on JavaFX thread
                Platform.runLater(() -> updateCube(accel, gyro));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            comPort.closePort();
        }
    }

    private void updateCube(double[] accel, double[] gyro) {
        // Integrate acceleration to velocity (very basic, for demo purposes)
        cubeVelX += accel[0];
        cubeVelY += accel[1];
        cubeVelZ += accel[2];

        // Update position
        cube.setTranslateX(cube.getTranslateX() + cubeVelX * 0.01); // scale down for demo
        cube.setTranslateY(cube.getTranslateY() + cubeVelY * 0.01);
        cube.setTranslateZ(cube.getTranslateZ() + cubeVelZ * 0.01);

        // Update rotation (additive, simple demo)
        cube.setRotationAxis(Rotate.X_AXIS);
        cube.setRotate(cube.getRotate() + gyro[0]);

    }

    /**
     * Moves the camera in the direction it's facing.
     * @param camera The PerspectiveCamera node.
     * @param cameraHolder The Group holding the camera (for yaw rotation).
     * @param dx Movement along X axis.
     * @param dy Movement along Y axis.
     * @param dz Movement along Z axis (forward/backward).
     */
    private void moveCamera(PerspectiveCamera camera, Group cameraHolder, double dx, double dy, double dz) {
        // Calculate yaw in radians
        double yawRad = Math.toRadians(cameraHolder.getRotate());
        // Calculate forward/backward movement
        double forwardX = Math.sin(yawRad) * dz;
        double forwardZ = Math.cos(yawRad) * dz;
        // Calculate left/right movement
        double strafeX = -Math.sin(yawRad - (Math.PI / 2)) * dx;
        double strafeZ = -Math.cos(yawRad - (Math.PI / 2)) * dx;

        camera.setTranslateX(camera.getTranslateX() + forwardX + strafeX);
        camera.setTranslateY(camera.getTranslateY() + dy);
        camera.setTranslateZ(camera.getTranslateZ() + forwardZ + strafeZ);
    }

    public static void main(String[] args) {
        launch(args);
    }
}