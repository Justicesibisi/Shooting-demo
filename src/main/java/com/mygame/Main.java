package com.mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.MouseButton;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.collision.CollisionResults;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;

public class Main extends SimpleApplication {

    private BulletAppState bulletAppState;
    
    // Player variables
    private CharacterControl player; // Player control
    private Vector3f walkDirection = new Vector3f(); // Player movement direction
    private boolean left = false, right = false, forward = false, backward = false;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // Initialize BulletAppState for physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        
        // Load player model or create a box representing the player
        Box playerBox = new Box(1, 2, 1); // Player's size
        Geometry playerGeom = new Geometry("Player", playerBox);
        Material playerMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        playerMat.setColor("Color", ColorRGBA.Red); // Player color
        playerGeom.setMaterial(playerMat);
        rootNode.attachChild(playerGeom);

        // Create player physical control
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
        player = new CharacterControl(capsuleShape, 0.05f);
        player.setPhysicsLocation(new Vector3f(0, 5, 0)); // Start position
        bulletAppState.getPhysicsSpace().add(player);

        // PlayerMovement defining keys
        inputManager.addMapping("Left", new KeyTrigger(keyInput.KEY_A)); // A for left
        inputManager.addMapping("Right", new KeyTrigger(keyInput.KEY_D)); // D for right
        inputManager.addMapping("Forward", new KeyTrigger(keyInput.KEY_W)); // W for forward
        inputManager.addMapping("Backward", new KeyTrigger(keyInput.KEY_S)); // S for backward

        inputManager.addListener(actionListener, "Left", "Right", "Forward", "Backward");

        // Shooting
        inputManager.addMapping("Shoot", new MouseButtonTrigger(MouseButton.LEFT)); // Left-click to shoot
        inputManager.addListener(actionListener, "Shoot");

        // Attach the player geometry to the root node
        rootNode.attachChild(playerGeom);

        // Adding a ground plane
        createGround();

        // Create walls
        createWalls();

        // Create pillars
        createPillars();

        // Create other objects like boxes or spheres
        createObjects();

        // Set the camera position
        cam.setLocation(new Vector3f(0, 3, 15));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        // Adjust flyCam settings for better navigation
        flyCam.setMoveSpeed(10);
    }

    private ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Select") && isPressed) {
                // Pick to see what object is clicked
                Vector3f click3d = cam.getWorldCoordinates(inputManager.getCursorPosition(), 0.0f).clone();
                Vector3f dir = cam.getWorldCoordinates(inputManager.getCursorPosition(), 0.3f).subtractLocal(click3d).normalizeLocal();
                Ray ray = new Ray(click3d, dir);

                // Check for collisions
                CollisionResults results = new CollisionResults();
                rootNode.collideWith(ray, results); // Check for collisions with objects in the scene

                if (results.size() > 0) {
                    // Get the first object hit
                    Geometry hitGeometry = results.getClosestCollision().getGeometry();
                    changeColor(hitGeometry); // Change the color of the hit object
                }
            }

            // Player movement controls
            if (name.equals("Left")) {
                left = isPressed;
            } else if (name.equals("Right")) {
                right = isPressed;
            } else if (name.equals("Forward")) {
                forward = isPressed;
            } else if (name.equals("Backward")) {
                backward = isPressed;
            }

            // Shooting
            if (name.equals("Shoot") && !isPressed) {
                shootBullet();
            }
        }

        private void shootBullet() {
            // Create a small sphere (bullet)
            Sphere bullet = new Sphere(16, 16, 0.2f);
            Geometry bulletGeom = new Geometry("Bullet", bullet);
            Material bulletMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            bulletMat.setColor("Color", ColorRGBA.Yellow); // Bullet color
            bulletGeom.setMaterial(bulletMat);

            // Position the bullet at the player's location
            bulletGeom.setLocalTranslation(cam.getLocation());

            // Add physics to the bullet
            RigidBodyControl bulletPhysics = new RigidBodyControl(1);
            bulletGeom.addControl(bulletPhysics);
            bulletPhysics.setLinearVelocity(cam.getDirection().mult(25)); // Shoot the bullet forward

            rootNode.attachChild(bulletGeom);
            bulletAppState.getPhysicsSpace().add(bulletPhysics);
        }
    };

    private void changeColor(Geometry geom) {
        // Change the color of the clicked geometry
        Material newMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        ColorRGBA newColor = ColorRGBA.randomColor(); // Generate a random color
        newMat.setColor("Color", newColor);
        geom.setMaterial(newMat); // Apply the new material
    }

    @Override
    public void simpleUpdate(float tpf) {
        // Game logic 
        Vector3f camDir = cam.getDirection().clone().multLocal(0.6f);
        Vector3f camLeft = cam.getLeft().clone().multLocal(0.4f);
        walkDirection.set(0, 0, 0);

        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (forward) {
            walkDirection.addLocal(camDir);
        }
        if (backward) {
            walkDirection.addLocal(camDir.negate());
        }

        player.setWalkDirection(walkDirection); // Apply the movement
        cam.setLocation(player.getPhysicsLocation());
    }

    @Override
    public void simpleRender(RenderManager rm) {
        // Render logic here
    }

    private void createGround() {
        Box groundBox = new Box(20, 1, 20); // A large box to represent the ground
        Geometry groundGeom = new Geometry("Ground", groundBox);
        Material groundMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture groundTex = assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg"); // Example texture
        groundMat.setTexture("ColorMap", groundTex);
        groundGeom.setMaterial(groundMat);
        groundGeom.setLocalTranslation(0, -1, 0);

        // Add static physics to ground
        RigidBodyControl groundPhysics = new RigidBodyControl(0);
        groundGeom.addControl(groundPhysics);
        bulletAppState.getPhysicsSpace().add(groundPhysics);

        rootNode.attachChild(groundGeom); // Attach ground to the scene
    }

    private void createWalls() {
        // Wall 1 (back wall)
        Box wallBox = new Box(10, 3, 0.1f); // Create a large flat box for walls
        Geometry wall1Geom = new Geometry("Wall1", wallBox);
        Material wallMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture wallTex = assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg");
        wallMat.setTexture("ColorMap", wallTex);
        wall1Geom.setMaterial(wallMat);
        wall1Geom.setLocalTranslation(0, 1, -10);

        rootNode.attachChild(wall1Geom); // Attach to the scene

        // Wall 2 (left wall)
        Geometry wall2Geom = wall1Geom.clone();
        wall2Geom.setLocalTranslation(-10, 1, 0);
        wall2Geom.rotate(0, FastMath.PI / 2, 0);

        rootNode.attachChild(wall2Geom); // Attach to the scene

        // Wall 3 (right wall)
        Geometry wall3Geom = wall1Geom.clone();
        wall3Geom.setLocalTranslation(10, 1, 0);
        wall3Geom.rotate(0, -FastMath.PI / 2, 0);

        rootNode.attachChild(wall3Geom); // Attach to the scene

        // Wall 4 (front wall)
        Geometry wall4Geom = wall1Geom.clone();
        wall4Geom.setLocalTranslation(0, 1, 10);
        wall4Geom.rotate(0, FastMath.PI, 0);

        rootNode.attachChild(wall4Geom); // Attach to the scene
    }

    private void createPillars() {
       for (int i = -8; i <= 8; i += 4) {
        // Create a cylinder with the correct parameters
        Cylinder pillar = new Cylinder(8, 12, 0.5f, 1.0f, true); // Specify radius and height
        Geometry pillarGeom = new Geometry("Pillar" + i, pillar);
        Material pillarMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        pillarMat.setColor("Color", ColorRGBA.Gray); // Set pillar color
        pillarGeom.setMaterial(pillarMat);
        pillarGeom.setLocalTranslation(i, 1.5f, 0); // Set position of pillar

        rootNode.attachChild(pillarGeom); // Attach to the scene
    }
}

    private void createObjects() {
        for (int i = -5; i <= 5; i += 2) {
            for (int j = -5; j <= 5; j += 2) {
                Box box = new Box(0.5f, 0.5f, 0.5f); // Create a box
                Geometry boxGeom = new Geometry("Box" + i + j, box);
                Material boxMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                boxMat.setColor("Color", ColorRGBA.Blue); // Set box color
                boxGeom.setMaterial(boxMat);
                boxGeom.setLocalTranslation(i, 0.5f, j); // Set position of box

                rootNode.attachChild(boxGeom); // Attach to the scene
            }
        }
    }
}
