/*
 * Copyright (c) 2014. William Mora
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.unocode.earthlingrun.stages;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.unocode.earthlingrun.actors.Score;
import com.unocode.earthlingrun.actors.menu.AboutButton;
import com.unocode.earthlingrun.actors.menu.GameLabel;
import com.unocode.earthlingrun.actors.menu.MusicButton;
import com.unocode.earthlingrun.actors.menu.SoundButton;
import com.unocode.earthlingrun.utils.WorldUtils;
import com.unocode.earthlingrun.actors.Background;
import com.unocode.earthlingrun.actors.Enemy;
import com.unocode.earthlingrun.actors.Ground;
import com.unocode.earthlingrun.actors.Runner;
import com.unocode.earthlingrun.actors.menu.AboutLabel;
import com.unocode.earthlingrun.actors.menu.AchievementsButton;
import com.unocode.earthlingrun.actors.menu.LeaderboardButton;
import com.unocode.earthlingrun.actors.menu.PauseButton;
import com.unocode.earthlingrun.actors.menu.PausedLabel;
import com.unocode.earthlingrun.actors.menu.ShareButton;
import com.unocode.earthlingrun.actors.menu.StartButton;
import com.unocode.earthlingrun.actors.menu.Tutorial;
import com.unocode.earthlingrun.enums.Difficulty;
import com.unocode.earthlingrun.enums.GameState;
import com.unocode.earthlingrun.utils.AudioUtils;
import com.unocode.earthlingrun.utils.BodyUtils;
import com.unocode.earthlingrun.utils.Constants;
import com.unocode.earthlingrun.utils.GameManager;

public class GameStage extends Stage implements ContactListener {

    private static final int VIEWPORT_WIDTH = com.unocode.earthlingrun.utils.Constants.APP_WIDTH;
    private static final int VIEWPORT_HEIGHT = com.unocode.earthlingrun.utils.Constants.APP_HEIGHT;

    private World world;
    private com.unocode.earthlingrun.actors.Ground ground;
    private com.unocode.earthlingrun.actors.Runner runner;

    private final float TIME_STEP = 1 / 300f;
    private float accumulator = 0f;

    private OrthographicCamera camera;

    private Rectangle screenLeftSide;
    private Rectangle screenRightSide;

    private SoundButton soundButton;
    private MusicButton musicButton;
    private com.unocode.earthlingrun.actors.menu.PauseButton pauseButton;
    private com.unocode.earthlingrun.actors.menu.StartButton startButton;
    private com.unocode.earthlingrun.actors.menu.LeaderboardButton leaderboardButton;
    private AboutButton aboutButton;
    private com.unocode.earthlingrun.actors.menu.ShareButton shareButton;
    private com.unocode.earthlingrun.actors.menu.AchievementsButton achievementsButton;

    private Score score;
    private float totalTimePassed;
    private boolean tutorialShown;

    private Vector3 touchPoint;

    public GameStage() {
        super(new ScalingViewport(Scaling.stretch, VIEWPORT_WIDTH, VIEWPORT_HEIGHT,
                new OrthographicCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)));
        setUpCamera();
        setUpStageBase();
        setUpGameLabel();
        setUpMainMenu();
        setUpTouchControlAreas();
        Gdx.input.setInputProcessor(this);
        AudioUtils.getInstance().init();
        onGameOver();
    }

    private void setUpStageBase() {
        setUpWorld();
        setUpFixedMenu();
    }

    private void setUpGameLabel() {
        Rectangle gameLabelBounds = new Rectangle(0, getCamera().viewportHeight * 7 / 8,
                getCamera().viewportWidth, getCamera().viewportHeight / 4);
        addActor(new GameLabel(gameLabelBounds));
    }

    private void setUpAboutText() {
        Rectangle gameLabelBounds = new Rectangle(0, getCamera().viewportHeight * 5 / 8,
                getCamera().viewportWidth, getCamera().viewportHeight / 4);
        addActor(new AboutLabel(gameLabelBounds));
    }

    /**
     * These menu buttons are always displayed
     */
    private void setUpFixedMenu() {
        setUpSound();
        setUpMusic();
        setUpScore();
    }

    private void setUpSound() {
        Rectangle soundButtonBounds = new Rectangle(getCamera().viewportWidth / 64,
                getCamera().viewportHeight * 13 / 20, getCamera().viewportHeight / 10,
                getCamera().viewportHeight / 10);
        soundButton = new SoundButton(soundButtonBounds);
        addActor(soundButton);
    }

    private void setUpMusic() {
        Rectangle musicButtonBounds = new Rectangle(getCamera().viewportWidth / 64,
                getCamera().viewportHeight * 4 / 5, getCamera().viewportHeight / 10,
                getCamera().viewportHeight / 10);
        musicButton = new MusicButton(musicButtonBounds);
        addActor(musicButton);
    }

    private void setUpScore() {
        Rectangle scoreBounds = new Rectangle(getCamera().viewportWidth * 47 / 64,
                getCamera().viewportHeight * 57 / 64, getCamera().viewportWidth / 4,
                getCamera().viewportHeight / 8);
        score = new Score(scoreBounds);
        addActor(score);
    }

    private void setUpPause() {
        Rectangle pauseButtonBounds = new Rectangle(getCamera().viewportWidth / 64,
                getCamera().viewportHeight * 1 / 2, getCamera().viewportHeight / 10,
                getCamera().viewportHeight / 10);
        pauseButton = new com.unocode.earthlingrun.actors.menu.PauseButton(pauseButtonBounds, new GamePauseButtonListener());
        addActor(pauseButton);
    }

    /**
     * These menu buttons are only displayed when the game is over
     */
    private void setUpMainMenu() {
        setUpStart();
        setUpLeaderboard();
        setUpAbout();
        setUpShare();
        setUpAchievements();
    }

    private void setUpStart() {
        Rectangle startButtonBounds = new Rectangle(getCamera().viewportWidth * 3 / 16,
                getCamera().viewportHeight / 4, getCamera().viewportWidth / 4,
                getCamera().viewportWidth / 4);
        startButton = new com.unocode.earthlingrun.actors.menu.StartButton(startButtonBounds, new GameStartButtonListener());
        addActor(startButton);
    }

    private void setUpLeaderboard() {
        Rectangle leaderboardButtonBounds = new Rectangle(getCamera().viewportWidth * 9 / 16,
                getCamera().viewportHeight / 4, getCamera().viewportWidth / 4,
                getCamera().viewportWidth / 4);
        leaderboardButton = new com.unocode.earthlingrun.actors.menu.LeaderboardButton(leaderboardButtonBounds,
                new GameLeaderboardButtonListener());
        addActor(leaderboardButton);
    }

    private void setUpAbout() {
        Rectangle aboutButtonBounds = new Rectangle(getCamera().viewportWidth * 23 / 25,
                getCamera().viewportHeight * 11 / 20, getCamera().viewportHeight / 10,//was 13
                getCamera().viewportHeight / 10);
        aboutButton = new AboutButton(aboutButtonBounds, new GameAboutButtonListener());
        addActor(aboutButton);
    }

    private void setUpShare() {
        Rectangle shareButtonBounds = new Rectangle(getCamera().viewportWidth / 64,
                getCamera().viewportHeight / 2, getCamera().viewportHeight / 10,
                getCamera().viewportHeight / 10);
        shareButton = new com.unocode.earthlingrun.actors.menu.ShareButton(shareButtonBounds, new GameShareButtonListener());
        addActor(shareButton);
    }

    private void setUpAchievements() {
        Rectangle achievementsButtonBounds = new Rectangle(getCamera().viewportWidth * 23 / 25,
                getCamera().viewportHeight * 8/ 20, getCamera().viewportHeight / 10,
                getCamera().viewportHeight / 10);
        achievementsButton = new com.unocode.earthlingrun.actors.menu.AchievementsButton(achievementsButtonBounds,
                new GameAchievementsButtonListener());
        addActor(achievementsButton);
    }

    private void setUpWorld() {
        world = WorldUtils.createWorld();
        world.setContactListener(this);
        setUpBackground();
        setUpGround();
    }

    private void setUpBackground() {
        addActor(new Background());
    }

    private void setUpGround() {
        ground = new Ground(WorldUtils.createGround(world));
        addActor(ground);
    }

    private void setUpCharacters() {
        setUpRunner();
        setUpPauseLabel();
        createEnemy();
    }

    private void setUpRunner() {
        if (runner != null) {
            runner.remove();
        }
        runner = new Runner(WorldUtils.createRunner(world));
        addActor(runner);
    }

    private void setUpCamera() {
        camera = new OrthographicCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0f);
        camera.update();
    }

    private void setUpTouchControlAreas() {
        touchPoint = new Vector3();
        screenLeftSide = new Rectangle(0, 0, getCamera().viewportWidth / 2,
                getCamera().viewportHeight);
        screenRightSide = new Rectangle(getCamera().viewportWidth / 2, 0,
                getCamera().viewportWidth / 2, getCamera().viewportHeight);
    }

    private void setUpPauseLabel() {
        Rectangle pauseLabelBounds = new Rectangle(0, getCamera().viewportHeight * 7 / 8,
                getCamera().viewportWidth, getCamera().viewportHeight / 4);
        addActor(new PausedLabel(pauseLabelBounds));
    }

    private void setUpTutorial() {
        if (tutorialShown) {
            return;
        }
        setUpLeftTutorial();
        setUpRightTutorial();
        tutorialShown = true;
    }

    private void setUpLeftTutorial() {
        float width = getCamera().viewportHeight / 4;
        float x = getCamera().viewportWidth / 4 - width / 2;
        Rectangle leftTutorialBounds = new Rectangle(x, getCamera().viewportHeight * 9 / 20, width,
                width);
        addActor(new com.unocode.earthlingrun.actors.menu.Tutorial(leftTutorialBounds, com.unocode.earthlingrun.utils.Constants.TUTORIAL_LEFT_REGION_NAME,
                com.unocode.earthlingrun.utils.Constants.TUTORIAL_LEFT_TEXT));
    }

    private void setUpRightTutorial() {
        float width = getCamera().viewportHeight / 4;
        float x = getCamera().viewportWidth * 3 / 4 - width / 2;
        Rectangle rightTutorialBounds = new Rectangle(x, getCamera().viewportHeight * 9 / 20, width,
                width);
        addActor(new Tutorial(rightTutorialBounds, com.unocode.earthlingrun.utils.Constants.TUTORIAL_RIGHT_REGION_NAME,
                Constants.TUTORIAL_RIGHT_TEXT));
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (com.unocode.earthlingrun.utils.GameManager.getInstance().getGameState() == com.unocode.earthlingrun.enums.GameState.PAUSED) return;

        if (com.unocode.earthlingrun.utils.GameManager.getInstance().getGameState() == com.unocode.earthlingrun.enums.GameState.RUNNING) {
            totalTimePassed += delta;
            updateDifficulty();
        }

        Array<Body> bodies = new Array<Body>(world.getBodyCount());
        world.getBodies(bodies);

        for (Body body : bodies) {
            update(body);
        }

        // Fixed timestep
        accumulator += delta;

        while (accumulator >= delta) {
            world.step(TIME_STEP, 6, 2);
            accumulator -= TIME_STEP;
        }

        //TODO: Implement interpolation

    }

    private void update(Body body) {
        if (!com.unocode.earthlingrun.utils.BodyUtils.bodyInBounds(body)) {
            if (com.unocode.earthlingrun.utils.BodyUtils.bodyIsEnemy(body) && !runner.isHit()) {
                createEnemy();
            }
            world.destroyBody(body);
        }
    }

    private void createEnemy() {
        com.unocode.earthlingrun.actors.Enemy enemy = new Enemy(WorldUtils.createEnemy(world));
        enemy.getUserData().setLinearVelocity(
                com.unocode.earthlingrun.utils.GameManager.getInstance().getDifficulty().getEnemyLinearVelocity());
        addActor(enemy);
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {

        // Need to get the actual coordinates
        translateScreenToWorldCoordinates(x, y);

        // If a menu control was touched ignore the rest
        if (menuControlTouched(touchPoint.x, touchPoint.y)) {
            return super.touchDown(x, y, pointer, button);
        }

        if (com.unocode.earthlingrun.utils.GameManager.getInstance().getGameState() != com.unocode.earthlingrun.enums.GameState.RUNNING) {
            return super.touchDown(x, y, pointer, button);
        }

        if (rightSideTouched(touchPoint.x, touchPoint.y)) {
            runner.jump();
        } else if (leftSideTouched(touchPoint.x, touchPoint.y)) {
            runner.dodge();
        }

        return super.touchDown(x, y, pointer, button);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {

        if (com.unocode.earthlingrun.utils.GameManager.getInstance().getGameState() != com.unocode.earthlingrun.enums.GameState.RUNNING) {
            return super.touchUp(screenX, screenY, pointer, button);
        }

        if (runner.isDodging()) {
            runner.stopDodge();
        }

        return super.touchUp(screenX, screenY, pointer, button);
    }

    private boolean menuControlTouched(float x, float y) {
        boolean touched = false;

        switch (com.unocode.earthlingrun.utils.GameManager.getInstance().getGameState()) {
            case OVER:
                touched = startButton.getBounds().contains(x, y)
                        || leaderboardButton.getBounds().contains(x, y)
                        || aboutButton.getBounds().contains(x, y);
                break;
            case RUNNING:
            case PAUSED:
                touched = pauseButton.getBounds().contains(x, y);
                break;
        }

        return touched || soundButton.getBounds().contains(x, y)
                || musicButton.getBounds().contains(x, y);
    }

    private boolean rightSideTouched(float x, float y) {
        return screenRightSide.contains(x, y);
    }

    private boolean leftSideTouched(float x, float y) {
        return screenLeftSide.contains(x, y);
    }

    /**
     * Helper function to get the actual coordinates in my world
     *
     * @param x
     * @param y
     */
    private void translateScreenToWorldCoordinates(int x, int y) {
        getCamera().unproject(touchPoint.set(x, y, 0));
    }

    @Override
    public void beginContact(Contact contact) {

        Body a = contact.getFixtureA().getBody();
        Body b = contact.getFixtureB().getBody();

        if ((com.unocode.earthlingrun.utils.BodyUtils.bodyIsRunner(a) && com.unocode.earthlingrun.utils.BodyUtils.bodyIsEnemy(b)) ||
                (com.unocode.earthlingrun.utils.BodyUtils.bodyIsEnemy(a) && com.unocode.earthlingrun.utils.BodyUtils.bodyIsRunner(b))) {
            if (runner.isHit() || runner.isPowerStomping()) {
                System.out.println("rnnner.isHit() || runner.isPowerStomping()");
                return;
            }
            System.out.println("runner.isPowerStomping():" + runner.isPowerStomping());
            runner.hit();
            //displayAd();
            int thisGameScore = score.getScore();
            int thisGameMult = score.getMultiplier();
            int thisGameJumpCount = runner.getJumpCount();
            int thisGameDoubleJumpCount = runner.getDoubleJumpCount();
            int thisGamePowerStompCount = runner.getPowerStompCount();
            int difficulty = com.unocode.earthlingrun.utils.GameManager.getInstance().getDifficulty().getLevel();
            int difficultyScale = com.unocode.earthlingrun.utils.GameManager.getInstance().getDifficultyScale();
            com.unocode.earthlingrun.utils.GameManager.getInstance().submitScore(thisGameScore);
            onGameOver();
            com.unocode.earthlingrun.utils.GameManager.getInstance().addGamePlayed();
            com.unocode.earthlingrun.utils.GameManager.getInstance().addJumpCount(thisGameJumpCount);
            com.unocode.earthlingrun.utils.GameManager.getInstance().addDoubleJumpCount(thisGameDoubleJumpCount);
            com.unocode.earthlingrun.utils.GameManager.getInstance().addPowerStompCount(thisGamePowerStompCount);
            System.out.println("GameEnd - score:" + thisGameScore + " | multi:" + thisGameMult + " | jumps:" + thisGameJumpCount + " | double jumps:" + thisGameDoubleJumpCount + " | power stomps:" + thisGamePowerStompCount + " | difficulty:" + difficulty + " | diffculty scale:" + difficultyScale);

        } else if ((com.unocode.earthlingrun.utils.BodyUtils.bodyIsRunner(a) && com.unocode.earthlingrun.utils.BodyUtils.bodyIsGround(b)) ||
                (com.unocode.earthlingrun.utils.BodyUtils.bodyIsGround(a) && BodyUtils.bodyIsRunner(b))) {
            runner.landed();
        }

    }

    private void updateDifficulty() {

        if (com.unocode.earthlingrun.utils.GameManager.getInstance().isMaxDifficulty()) {
            return;
        }

        com.unocode.earthlingrun.enums.Difficulty currentDifficulty = com.unocode.earthlingrun.utils.GameManager.getInstance().getDifficulty();

        com.unocode.earthlingrun.utils.GameManager.getInstance().setDifficultyScale(1);
        if (totalTimePassed > com.unocode.earthlingrun.utils.GameManager.getInstance().getDifficulty().getLevel() * com.unocode.earthlingrun.utils.GameManager.getInstance().getDifficultyScale()) {

            int nextDifficulty = currentDifficulty.getLevel() + 1;
            String difficultyName = "DIFFICULTY_" + nextDifficulty;
            com.unocode.earthlingrun.utils.GameManager.getInstance().setDifficulty(Difficulty.valueOf(difficultyName));

            runner.onDifficultyChange(com.unocode.earthlingrun.utils.GameManager.getInstance().getDifficulty());
            score.setMultiplier(com.unocode.earthlingrun.utils.GameManager.getInstance().getDifficulty().getScoreMultiplier());

            //displayAd();
        }

    }

    //private void displayAd() {
        //GameManager.getInstance().displayAd();
    //}

    @Override
    public void endContact(Contact contact) {

    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }

    private class GamePauseButtonListener implements PauseButton.PauseButtonListener {

        @Override
        public void onPause() {
            onGamePaused();
        }

        @Override
        public void onResume() {
            onGameResumed();
        }

    }

    private class GameStartButtonListener implements StartButton.StartButtonListener {

        @Override
        public void onStart() {
            clear();
            setUpStageBase();
            setUpCharacters();
            setUpPause();
            setUpTutorial();
            onGameResumed();
        }

    }

    private class GameLeaderboardButtonListener
            implements LeaderboardButton.LeaderboardButtonListener {

        @Override
        public void onLeaderboard() {
            com.unocode.earthlingrun.utils.GameManager.getInstance().displayLeaderboard();
        }

    }

    private class GameAboutButtonListener implements AboutButton.AboutButtonListener {

        @Override
        public void onAbout() {
            if (com.unocode.earthlingrun.utils.GameManager.getInstance().getGameState() == com.unocode.earthlingrun.enums.GameState.OVER) {
                onGameAbout();
            } else {
                clear();
                setUpStageBase();
                setUpGameLabel();
                onGameOver();
            }
        }

    }

    private class GameShareButtonListener implements ShareButton.ShareButtonListener {

        @Override
        public void onShare() {
            com.unocode.earthlingrun.utils.GameManager.getInstance().share();
        }

    }

    private class GameAchievementsButtonListener
            implements AchievementsButton.AchievementsButtonListener {

        @Override
        public void onAchievements() {
            com.unocode.earthlingrun.utils.GameManager.getInstance().displayAchievements();
        }

    }

    private void onGamePaused() {
        com.unocode.earthlingrun.utils.GameManager.getInstance().setGameState(com.unocode.earthlingrun.enums.GameState.PAUSED);
    }

    private void onGameResumed() {
        com.unocode.earthlingrun.utils.GameManager.getInstance().setGameState(com.unocode.earthlingrun.enums.GameState.RUNNING);
    }

    private void onGameOver() {
        com.unocode.earthlingrun.utils.GameManager.getInstance().setGameState(com.unocode.earthlingrun.enums.GameState.OVER);
        com.unocode.earthlingrun.utils.GameManager.getInstance().resetDifficulty();
        totalTimePassed = 0;
        setUpMainMenu();
    }

    private void onGameAbout() {
        GameManager.getInstance().setGameState(GameState.ABOUT);
        clear();
        setUpStageBase();
        setUpGameLabel();
        setUpAboutText();
        setUpAbout();
    }

}