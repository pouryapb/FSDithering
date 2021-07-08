package basics;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

public class Game extends Canvas implements Runnable {

	private static final long serialVersionUID = -7059204873191017442L;
	public static final int WIDTH = 524;
	public static final int HEIGHT = 350 * 2;
	private Thread thread;
	private boolean running = false;
	private BufferedImage image;
	private BufferedImage dither;
	private static final Logger LOGGER = Logger.getLogger(Game.class.getName());

	public Game() {

		var loader = new BufferedImageLoader();
		image = loader.loadImage("res/p1.jpg");
		dither = loader.loadImage("res/p1.jpg");
		dither = filterBW(dither);

		var factor = 4;

		for (var y = 0; y < dither.getHeight() - 1; y++) {
			for (var x = 1; x < dither.getWidth() - 1; x++) {

				Color pixel = new Color(dither.getRGB(x, y));

				int oldR = pixel.getRed();
				int oldG = pixel.getGreen();
				int oldB = pixel.getBlue();

				int newR = findClosestPalleteColor(oldR, factor);
				int newG = findClosestPalleteColor(oldG, factor);
				int newB = findClosestPalleteColor(oldB, factor);

				int errR = oldR - newR;
				int errG = oldG - newG;
				int errB = oldB - newB;

				dither.setRGB(x, y, new Color(newR, newG, newB).getRGB());
				try {
					dither.setRGB(x + 1, y,
							(quantize(new Color(dither.getRGB(x + 1, y)), errR, errG, errB, 7)).getRGB());
					dither.setRGB(x - 1, y + 1,
							(quantize(new Color(dither.getRGB(x - 1, y + 1)), errR, errG, errB, 3)).getRGB());
					dither.setRGB(x, y + 1,
							(quantize(new Color(dither.getRGB(x, y + 1)), errR, errG, errB, 5)).getRGB());
					dither.setRGB(x + 1, y + 1,
							(quantize(new Color(dither.getRGB(x + 1, y + 1)), errR, errG, errB, 1)).getRGB());
				} catch (Exception e) {
					// do nothing
				}

			}
		}

		var file = new File(getClass().getResource("res/pRes.jpg").getFile());
		try {
			ImageIO.write(dither, "jpg", file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		new Window(WIDTH, HEIGHT, "Dithering!", this);
	}

	public synchronized void start() {
		thread = new Thread(this);
		thread.start();
		running = true;
	}

	public synchronized void stop() {
		try {
			thread.join();
			running = false;
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}

	}

	public void run() {
		this.requestFocus();
		long lastTime = System.nanoTime();
		var amountOfTicks = 60.0;
		double ns = 1000000000 / amountOfTicks;
		double delta = 0;
		long timer = System.currentTimeMillis();
		var frames = 0;
		while (running) {
			long now = System.nanoTime();
			delta += (now - lastTime) / ns;
			lastTime = now;
			while (delta >= 1) {
				tick();
				delta--;
			}
			if (running)
				render();
			frames++;

			if (System.currentTimeMillis() - timer > 1000) {
				timer += 1000;
				var log = "FPS: " + frames;
				LOGGER.info(log);
				frames = 0;
			}
		}
		stop();
	}

	public void tick() {
		// not needed
	}

	public void render() {

		BufferStrategy bs = this.getBufferStrategy();
		if (bs == null) {
			this.createBufferStrategy(3);
			return;
		}

		Graphics g = bs.getDrawGraphics();
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		/////////////////////////////////////

		g.drawImage(image, 0, 0, null);
		g.drawImage(dither, 0, 350, null);

		/////////////////////////////////////
		g.dispose();
		bs.show();

	}

	public int findClosestPalleteColor(int val, int factor) {
		return (factor * val / 255) * (255 / factor);
	}

	public Color quantize(Color pixel, int errR, int errG, int errB, int p) {

		int r = pixel.getRed();
		int g = pixel.getGreen();
		int b = pixel.getBlue();

		r = r + errR * p / 16;
		g = g + errG * p / 16;
		b = b + errB * p / 16;

		return new Color(r, g, b);
	}

	public BufferedImage filterBW(BufferedImage image) {

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {

				Color color = new Color(image.getRGB(x, y));

				int r = color.getRed();
				int g = color.getGreen();
				int b = color.getBlue();

				r = g = b = (r + g + b) / 3;

				color = new Color(r, g, b);

				image.setRGB(x, y, color.getRGB());
			}
		}

		return image;
	}

	public static void main(String[] args) {
		new Game();
	}
}
