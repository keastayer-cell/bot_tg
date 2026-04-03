import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class PongGame extends JPanel {
    // Размеры поля
    private static final int WIDTH = 800;
    private static final int HEIGHT = 500;
    
    // Размеры ракеток
    private static final int PADDLE_WIDTH = 15;
    private static final int PADDLE_HEIGHT = 80;
    
    // Размеры мяча
    private static final int BALL_SIZE = 15;
    
    // Скорости
    private static final int PLAYER_SPEED = 6;
    private static final int COMPUTER_SPEED = 4;
    private static final int INITIAL_BALL_SPEED = 5;
    
    // Позиции ракеток
    private int playerY = HEIGHT / 2 - PADDLE_HEIGHT / 2;
    private int computerY = HEIGHT / 2 - PADDLE_HEIGHT / 2;
    
    // Позиция мяча
    private int ballX = WIDTH / 2;
    private int ballY = HEIGHT / 2;
    private int ballSpeedX = INITIAL_BALL_SPEED;
    private int ballSpeedY = INITIAL_BALL_SPEED;
    
    // Счёт
    private int playerScore = 0;
    private int computerScore = 0;
    
    // Состояние игры
    private boolean gameRunning = true;
    private boolean paused = false;
    
    // Поток игры
    private Thread gameThread;
    
    public PongGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        
        // Управление клавиатурой
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    playerY -= 20;
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    playerY += 20;
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    paused = !paused;
                } else if (e.getKeyCode() == KeyEvent.VK_R) {
                    resetGame();
                }
                
                // Ограничение движения ракетки игрока
                playerY = Math.max(0, Math.min(HEIGHT - PADDLE_HEIGHT, playerY));
            }
        });
        
        // Запуск игрового цикла
        gameThread = new Thread(this::gameLoop);
        gameThread.start();
    }
    
    private void gameLoop() {
        while (gameRunning) {
            if (!paused) {
                update();
            }
            repaint();
            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void update() {
        // Движение компьютера (простая логика ИИ)
        int computerCenter = computerY + PADDLE_HEIGHT / 2;
        if (computerCenter < ballY - 10) {
            computerY += COMPUTER_SPEED;
        } else if (computerCenter > ballY + 10) {
            computerY -= COMPUTER_SPEED;
        }
        computerY = Math.max(0, Math.min(HEIGHT - PADDLE_HEIGHT, computerY));
        
        // Движение мяча
        ballX += ballSpeedX;
        ballY += ballSpeedY;
        
        // Отскок от верхней и нижней стен
        if (ballY <= 0 || ballY >= HEIGHT - BALL_SIZE) {
            ballSpeedY = -ballSpeedY;
        }
        
        // Проверка столкновения с ракеткой игрока
        if (ballX <= PADDLE_WIDTH + 10 && 
            ballY + BALL_SIZE >= playerY && 
            ballY <= playerY + PADDLE_HEIGHT) {
            ballSpeedX = Math.abs(ballSpeedX) + 1;
            ballSpeedY = (ballY - (playerY + PADDLE_HEIGHT / 2)) / 5;
        }
        
        // Проверка столкновения с ракеткой компьютера
        if (ballX >= WIDTH - PADDLE_WIDTH - 10 - BALL_SIZE && 
            ballY + BALL_SIZE >= computerY && 
            ballY <= computerY + PADDLE_HEIGHT) {
            ballSpeedX = -(Math.abs(ballSpeedX) + 1);
            ballSpeedY = (ballY - (computerY + PADDLE_HEIGHT / 2)) / 5;
        }
        
        // Проверка на гол (мяч ушёл за левую или правую границу)
        if (ballX < 0) {
            computerScore++;
            resetBall();
        } else if (ballX > WIDTH) {
            playerScore++;
            resetBall();
        }
    }
    
    private void resetBall() {
        ballX = WIDTH / 2;
        ballY = HEIGHT / 2;
        ballSpeedX = (Math.random() > 0.5 ? 1 : -1) * INITIAL_BALL_SPEED;
        ballSpeedY = (Math.random() > 0.5 ? 1 : -1) * INITIAL_BALL_SPEED;
    }
    
    private void resetGame() {
        playerScore = 0;
        computerScore = 0;
        resetBall();
        playerY = HEIGHT / 2 - PADDLE_HEIGHT / 2;
        computerY = HEIGHT / 2 - PADDLE_HEIGHT / 2;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Рисуем центральную линию
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10, 10}, 0));
        g2d.drawLine(WIDTH / 2, 0, WIDTH / 2, HEIGHT);
        
        // Рисуем ракетку игрока
        g2d.setColor(Color.GREEN);
        g2d.fillRect(10, playerY, PADDLE_WIDTH, PADDLE_HEIGHT);
        
        // Рисуем ракетку компьютера
        g2d.setColor(Color.RED);
        g2d.fillRect(WIDTH - PADDLE_WIDTH - 10, computerY, PADDLE_WIDTH, PADDLE_HEIGHT);
        
        // Рисуем мяч
        g2d.setColor(Color.WHITE);
        g2d.fillOval(ballX, ballY, BALL_SIZE, BALL_SIZE);
        
        // Рисуем счёт
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.drawString(String.valueOf(playerScore), WIDTH / 4, 60);
        g2d.drawString(String.valueOf(computerScore), 3 * WIDTH / 4, 60);
        
        // Рисуем подсказки
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("UP/DOWN - движение | SPACE - пауза | R - сброс", 10, HEIGHT - 10);
        
        if (paused) {
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            g2d.drawString("ПАУЗА", WIDTH / 2 - 70, HEIGHT / 2);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Pong - Пинг Понг");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new PongGame());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
        });
    }
}
