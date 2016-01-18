package leapMotionJOGL;

import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;

import Jama.Matrix;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.leapmotion.leap.Bone;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Finger;
import com.leapmotion.leap.Hand;

class LeapKey implements GLEventListener {
	// region -------------- Variable --------------
	private GL2 gl;
	ArrayList<Double[][][]> dirFrame = new ArrayList<Double[][][]>();
	ArrayList<Double[][][]> PosFrame = new ArrayList<Double[][][]>();

	// [BoneNum][FingerNum][x,y,z]
	static Double[][][] PosNow = new Double[5][5][3];
	static Double[][][] dirNow = new Double[4][5][3];
	static Double[][][] dirBefInterval = new Double[4][5][3];
	double[] WidthSaved = new double[5], WidthNow = new double[5];
	double[] LengthNow = new double[5], LengthSaved = new double[5];
	int TimeCount = 0, BeforeSaveTime = 0, NowCheckingKeyPoint = 0,
			scanInterval = 60, checkStartTime, checkRemitTime = 3000,
			result = 0;
	static boolean isMoving = false, isHandExist = false, dataExported = false,
			dataInported = false, recodingMode = false, checkMode = false,
			getNextKeyPoint = false, saveTiming = false;
	static int SizeX = 1000, SizeY = 500;
	static DecimalFormat df2 = new DecimalFormat("##.###%");
	static Font font = new Font("Tahoma", java.awt.Font.PLAIN, 40);
	static TextRenderer tr = new TextRenderer(font, true, true);
	static java.awt.Frame frame;
	static Controller controller;
	static com.leapmotion.leap.Frame leapFrame;
	static LeapKey leapKey;
	private BufferedReader in;

	// endregion -------------- Variable --------------

	public static void main(String[] args) {
		controller = new Controller();
		leapFrame = controller.frame();
		frame = new java.awt.Frame("LeapLogin");
		GLCanvas canvas = new GLCanvas();
		leapKey = new LeapKey();
		canvas.addGLEventListener(leapKey);
		frame.add(canvas);
		frame.setSize(SizeX, SizeY);
		final Animator animator = new Animator(canvas);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				new Thread(new Runnable() {
					public void run() {
						animator.stop();
						System.exit(0);
					}
				}).start();
			}
		});
		frame.setVisible(true);
		animator.start();
	}

	public LeapKey() {
	}

	public void display(GLAutoDrawable drawable) {
		gl = drawable.getGL().getGL2();
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		getNowLeapData();
		drawButton(gl);
		drawHands(gl);
		if (TimeCount % scanInterval == 0)
			checkMove();
		printHandState();
		if (checkMode)
			CulcDiff();
		TimeCount++;
	}

	// region -------------- Display Methods --------------

	void getNowLeapData() {
		int iHand = 0;
		Double[][][] tempPos = new Double[5][5][3], tempDir = new Double[4][5][3];
		leapFrame = controller.frame();
		isHandExist = false;
		for (Hand hand : leapFrame.hands()) {
			isHandExist = true;
			int iFin = 0;
			for (Finger finger : hand.fingers()) {
				int iBon = 0;
				WidthNow[iFin] = finger.width();
				LengthNow[iFin] = finger.length();
				if (saveTiming && recodingMode) {
					WidthSaved[iFin] = finger.width();
					LengthSaved[iFin] = finger.length();
				}
				for (Bone.Type boneType : Bone.Type.values()) {
					Bone bone = finger.bone(boneType);
					if (saveTiming && recodingMode) {
						// copy xyz from right to left
						copyXYZ(tempDir[iBon][iFin], dirNow[iBon][iFin]);
						copyXYZ(tempPos[iBon][iFin], PosNow[iBon][iFin]);
						if (iBon == 3)
							copyXYZ(tempPos[iBon + 1][iFin],
									PosNow[iBon + 1][iFin]);
					}
					getBoneDir(dirNow[iBon][iFin], bone);
					getBonePos1(PosNow[iBon][iFin], bone);
					if (iBon == 3)
						getBonePos2(PosNow[iBon + 1][iFin], bone);
					iBon++;
				}
				iFin++;
			}
			iHand++;
		}
		if (saveTiming && recodingMode) {
			saveTiming = false;
			PosFrame.add(tempPos);
			dirFrame.add(tempDir);
		}
	}

	// region -------------- Button --------------
	void drawButton(GL2 gl) {
		double px = -4.0d, py = 1.7d, pz = -1.2d, w = 1d, h = 0.4d, sp = 0.2d, txSize = 0.007d;
		int num;
		startButton(gl, px + (w + sp) * (num = 0), py, pz, w, h);
		endButton(gl, px + (w + sp) * (++num), py, pz, w, h);
		clearButton(gl, px + (w + sp) * (++num), py, pz, w, h);
		checkstartButton(gl, px + (w + sp) * (++num), py, pz, w, h);
		exitButton(gl, px + (w + sp) * (++num), py, pz, w, h);
		saveButton(gl, px + (w + sp) * (++num), py, pz, w, h);
		loadButton(gl, px + (w + sp) * (++num), py, pz, w, h);
		tr.begin3DRendering();
		tr.setColor(1.0f, 1.0f, 1.0f, 1.0f);
		tr.draw3D("Register", (float) (px + (w + sp) * (num = 0)),
				(float) (py - h), (float) pz, (float) txSize);
		tr.draw3D(" End ", (float) (px + (w + sp) * (++num)), (float) (py - h),
				(float) pz, (float) txSize);
		tr.draw3D("Clear", (float) (px + (w + sp) * (++num)), (float) (py - h),
				(float) pz, (float) txSize);
		tr.draw3D("Check", (float) (px + (w + sp) * (++num)), (float) (py - h),
				(float) pz, (float) txSize);
		tr.draw3D("Exit ", (float) (px + (w + sp) * (++num)), (float) (py - h),
				(float) pz, (float) txSize);
		tr.draw3D("Save ", (float) (px + (w + sp) * (++num)), (float) (py - h),
				(float) pz, (float) txSize);
		tr.draw3D("Load ", (float) (px + (w + sp) * (++num)), (float) (py - h),
				(float) pz, (float) txSize);
		tr.end3DRendering();
	}

	void startButton(GL2 gl, double px, double py, double pz, double w, double h) {
		if (isPressed(px, py, pz, w, h)) {
			gl.glColor3f(1.0f, 0.0f, 0.0f);
			saveTiming = false;
			recodingMode = true;
			NowCheckingKeyPoint = 0;
			dataExported = false;
		} else
			gl.glColor3f(0.21f, 0.6f, 0.5f);
		drawRect(px, py, pz, w, h);
	}

	void endButton(GL2 gl, double px, double py, double pz, double w, double h) {
		if (isPressed(px, py, pz, w, h)) {
			gl.glColor3f(1.0f, 0.0f, 0.0f);
			saveTiming = false;
			recodingMode = false;
			NowCheckingKeyPoint = 0;
		} else
			gl.glColor3f(0.21f, 0.6f, 0.5f);
		drawRect(px, py, pz, w, h);
	}

	void clearButton(GL2 gl, double px, double py, double pz, double w, double h) {
		if (isPressed(px, py, pz, w, h)) {
			gl.glColor3f(0.0f, 1.0f, 0.0f);
			checkMode = false;
			NowCheckingKeyPoint = 0;
			dataExported = false;
			dataInported = false;
			for (int i = dirFrame.size() - 1; i >= 0; i--)
				dirFrame.remove(i);
			for (int i = PosFrame.size() - 1; i >= 0; i--)
				PosFrame.remove(i);
		} else
			gl.glColor3f(0.21f, 0.6f, 0.5f);
		drawRect(px, py, pz, w, h);
	}

	void checkstartButton(GL2 gl, double px, double py, double pz, double w,
			double h) {
		if (isPressed(px, py, pz, w, h)) {
			if (dirFrame.size() <= 0) {
				draw2Dtext("No Save Frame", 200, 20);
			} else {
				checkMode = true;
				checkStartTime = TimeCount;
				NowCheckingKeyPoint = 0;
				getNextKeyPoint = false;
				result = 0;
			}
			gl.glColor3f(1.0f, 0.0f, 0.0f);
		} else
			gl.glColor3f(0.21f, 0.6f, 0.5f);
		drawRect(px, py, pz, w, h);
	}

	void exitButton(GL2 gl, double px, double py, double pz, double w, double h) {
		if (isPressed(px, py, pz, w, h)) {
			gl.glColor3f(1.0f, 0.0f, 0.0f);
			System.exit(1);
		} else
			gl.glColor3f(0.21f, 0.6f, 0.5f);
		drawRect(px, py, pz, w, h);
	}

	void saveButton(GL2 gl, double px, double py, double pz, double w, double h) {
		if (isPressed(px, py, pz, w, h)) {
			gl.glColor3f(1.0f, 0.0f, 0.0f);
			dataInported = false;
			if (!dataExported) {
				outputData();
				dataExported = true;
			}
		} else
			gl.glColor3f(0.21f, 0.6f, 0.5f);
		drawRect(px, py, pz, w, h);
	}

	void loadButton(GL2 gl, double px, double py, double pz, double w, double h) {
		if (isPressed(px, py, pz, w, h)) {
			gl.glColor3f(1.0f, 0.0f, 0.0f);
			if (!dataInported)
				loadData();
			dataInported = true;
		} else
			gl.glColor3f(0.21f, 0.6f, 0.5f);
		drawRect(px, py, pz, w, h);
	}

	void drawRect(double px, double py, double pz, double w, double h) {
		gl.glBegin(5);
		gl.glVertex3d(px, py, pz);
		gl.glVertex3d(px + w, py, pz);
		gl.glVertex3d(px + w, py - h, pz);
		gl.glVertex3d(px, py - h, pz);
		gl.glVertex3d(px, py, pz);
		gl.glEnd();
	}

	boolean isPressed(double px, double py, double pz, double w, double h) {
		double x = PosNow[4][1][0].doubleValue() / 50.0d, y = (PosNow[4][1][1]
				.doubleValue() - (SizeY / 2)) / 100.0d, z = PosNow[4][1][2]
				.doubleValue() / 50.0d;
		return (px < x && x < px + w && py - h < y && y < py && z < pz);
	}

	void outputData() {
		try {
			File file1 = new File("data.txt"), file2 = new File(
					"static_data.txt");
			PrintWriter pw1 = new PrintWriter(new BufferedWriter(
					new FileWriter(file1))), pw2 = new PrintWriter(
					new BufferedWriter(new FileWriter(file2)));
			for (int num = 0; num < dirFrame.size(); num++) {
				pw1.println("startFrame");
				for (int Fin = 0, okFin = 0; Fin < 5; Fin++) {
					for (int Bone = 0; Bone < 4; Bone++) {
						writeXYZ(pw1, dirFrame.get(num)[Bone][Fin]);
					}
				}
			}
			pw1.close();

			for (int fin = 0; fin < 5; fin++) {
				pw2.println(LengthSaved[fin]);
				pw2.println(WidthSaved[fin]);
			}
			pw2.close();

		} catch (IOException e) {
			System.out.println(e);
		}
	}

	void loadData() {
		for (int i = dirFrame.size() - 1; i >= 0; i--)
			dirFrame.remove(i);

		for (int i = PosFrame.size() - 1; i >= 0; i--)
			PosFrame.remove(i);

		try {
			in = new BufferedReader(new FileReader("data.txt"));

			String line, start = "startFrame";

			while ((line = in.readLine()) != null) {
				System.out.println(line);
				Double[][][] tempDir = new Double[4][5][3];
				for (int Fin = 0; Fin < 5; Fin++) {
					for (int Bone = 0; Bone < 4; Bone++) {
						for (int xyz = 0; xyz < 3; xyz++) {
							if ((line = in.readLine()) != null) {
								tempDir[Bone][Fin][xyz] = new Double(Double.parseDouble(line));
							}
						}
					}
				}
				dirFrame.add(tempDir);
			}
			in = new BufferedReader(new FileReader("static_data.txt"));
			Double[][][] tempDir = new Double[4][5][3];
			for (int Fin = 0; Fin < 5; Fin++) {
				if ((line = in.readLine()) != null)
					LengthSaved[Fin] = Double.parseDouble(line);
				else
					return;
				if ((line = in.readLine()) != null)
					WidthSaved[Fin] = Double.parseDouble(line);
				else
					return;
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// endregion -------------- Button --------------

	void drawHands(GL2 gl) {
		gl.glLineWidth(4.0f);
		gl.glColor3f(0.6f, 0.5f, 1.0f);
		for (int iFin = 0; iFin < 5; iFin++) {
			gl.glBegin(3);
			for (int iBon = 0; iBon < 5; iBon++) {
				gl.glLineWidth(8.0f - iBon * 0.6f);
				gl.glVertex3d(
						PosNow[iBon][iFin][0].doubleValue() / 50.0d,
						(PosNow[iBon][iFin][1].doubleValue() - (SizeY / 2)) / 100.0d,
						PosNow[iBon][iFin][2].doubleValue() / 50.0d);
			}
			gl.glEnd();
		}
		gl.glPointSize(5f);
		gl.glColor3f(0.0f, 1.0f, 1.0f);
		for (int iFin = 0; iFin < 5; iFin++) {
			gl.glBegin(0);
			for (int iBon = 0; iBon < 5; iBon++) {
				gl.glLineWidth(8.0f - iBon * 0.6f);
				gl.glVertex3d(
						PosNow[iBon][iFin][0].doubleValue() / 50.0d,
						(PosNow[iBon][iFin][1].doubleValue() - (SizeY / 2)) / 100.0d,
						PosNow[iBon][iFin][2].doubleValue() / 50.0d);
			}
			gl.glEnd();
		}
		gl.glLineWidth(1.0f);
		gl.glColor4f(0.0f, 1.0f, 1.0f, 0.5f);
		for (int iBon = 0; iBon < 2; iBon++) {
			gl.glBegin(3);
			for (int iFin = 0; iFin < 5; iFin++) {
				gl.glVertex3d(PosNow[iBon + ((iFin == 0) ? 1 : 0)][iFin][0]
						.doubleValue() / 50.0d,
						(PosNow[iBon + ((iFin == 0) ? 1 : 0)][iFin][1]
								.doubleValue() - (SizeY / 2)) / 100.0d,
						PosNow[iBon + ((iFin == 0) ? 1 : 0)][iFin][2]
								.doubleValue() / 50.0d);
			}
			gl.glEnd();
		}
		gl.glLineWidth(3.0f);
		gl.glColor4f(1.0f, 0.0f, 0.0f, 0.5f);
		for (int frameNum = 0; frameNum < PosFrame.size(); frameNum++) {
			for (int Fin = 0; Fin < 5; Fin++) {
				gl.glBegin(3);
				for (int Bone = 0; Bone < 4; Bone++) {
					try {
						gl.glVertex3d(
								(PosFrame.get(frameNum))[Bone][Fin][0] / 50.0f,
								((PosFrame.get(frameNum))[Bone][Fin][1] - (SizeY / 2)) / 100.0f,
								(PosFrame.get(frameNum))[Bone][Fin][2] / 50.0f);
					} catch (Exception e) {
						System.out.print("error");
					}
				}
				gl.glEnd();
			}
		}
	}

	void checkMove() {
		boolean beforeHandMoving = isMoving;
		double sensitivity = 0.5d;
		isMoving = false;
		l1: for (int Fin = 0; Fin < 5; Fin++)
			for (int Bone = 0; Bone < 4; Bone++) {
				Matrix t = toMatrix(dirBefInterval[Bone][Fin]), n = toMatrix(dirNow[Bone][Fin]);
				if ((t.minus(n)).normF() > sensitivity) {
					isMoving = true;
					break l1;
				}
			}
		// scan Key
		if (beforeHandMoving && !isMoving && TimeCount - BeforeSaveTime > 100) {
			BeforeSaveTime = TimeCount;
			saveTiming = true;
		}
		for (int iFin = 0; iFin < 5; iFin++)
			for (int iBon = 0; iBon < 4; iBon++)
				copyXYZ(dirBefInterval[iBon][iFin], dirNow[iBon][iFin]);
	}

	void printHandState() {
		if (!isHandExist)
			draw2Dtext("NoHand", 20, 20);
		else if (isMoving)
			draw2Dtext("Moving", 20, 20);
		else if (!isMoving)
			draw2Dtext("Stopping", 20, 20);
	}

	void CulcDiff() {
		if (result == 1) {
			printResult("Clear");
		} else if (result == -1) {
			printResult("fail");
		} else if (result == 0) {
			if (TimeCount - checkStartTime > checkRemitTime)
				result = -1;
			else if (!isHandExist)
				result = -1;
			else {
				if (getNextKeyPoint) {
					NowCheckingKeyPoint = Math.min((NowCheckingKeyPoint + 1),
							dirFrame.size());
					getNextKeyPoint = false;
				}
				if (NowCheckingKeyPoint == dirFrame.size())
					result = 1;
				else {
					double okDouble = 0.95d;
					System.out.println(NowCheckingKeyPoint + 1 + "/"
							+ dirFrame.size());
					for (int Fin = 0, okFin = 0; Fin < 5; Fin++) {
						double innerProduct = 0.0;
						for (int Bone = 0; Bone < 4; Bone++) {
							Matrix n = toMatrix(dirNow[Bone][Fin]), s = toMatrix(dirFrame
									.get(NowCheckingKeyPoint)[Bone][Fin]);
							innerProduct += ((n).times(s.transpose()))
									.get(0, 0);
						}
						double th = innerProduct / ((Fin == 0) ? 3.0d : 4.0d);
						System.out.print("Fin." + Fin + " w:"
								+ df2.format(WidthNow[Fin] / WidthSaved[Fin]));
						System.out
								.print("l:"
										+ df2.format(LengthNow[Fin]
												/ LengthSaved[Fin]));
						System.out.println("th:" + df2.format(th));

						if (th >= okDouble)
							okFin++;
						if (okFin == 5)
							getNextKeyPoint = true;
					}
					System.out.println();
				}
			}
		}
	}

	// endregion -------------- Display Methods --------------

	// region -------------- Tools --------------
	void printResult(String s) {
		if (TimeCount - checkStartTime < checkRemitTime * 1.3)
			draw2Dtext(s, 200, 20);
		else
			checkMode = false;
	}

	void writeXYZ(PrintWriter pw, Double[] a) {
		pw.println(a[0].doubleValue());
		pw.println(a[1].doubleValue());
		pw.println(a[2].doubleValue());
	}

	void copyXYZ(Double[] to, Double[] from) {
		for (int xyz = 0; xyz < 3; xyz++)
			to[xyz] = from[xyz];
	}

	void draw2Dtext(String s, int x, int y) {
		tr.beginRendering(SizeX, SizeY);
		tr.setColor(1.0f, 1.0f, 1.0f, 1.0f);
		tr.draw(s, x, y);
		tr.endRendering();
	}

	void getBoneDir(Double[] a, Bone bone) {
		a[0] = new Double(bone.direction().getX());
		a[1] = new Double(bone.direction().getY());
		a[2] = new Double(bone.direction().getZ());
	}

	void getBonePos1(Double[] a, Bone bone) {
		a[0] = new Double(bone.prevJoint().getX());
		a[1] = new Double(bone.prevJoint().getY());
		a[2] = new Double(bone.prevJoint().getZ());
	}

	void getBonePos2(Double[] a, Bone bone) {
		a[0] = new Double(bone.nextJoint().getX());
		a[1] = new Double(bone.nextJoint().getY());
		a[2] = new Double(bone.nextJoint().getZ());
	}

	Matrix toMatrix(Double[] a) {
		return (new Matrix(new double[] { a[0].doubleValue(),
				a[1].doubleValue(), a[2].doubleValue() }, 1));
	}

	// endregion-------------- Tools --------------

	public void dispose(GLAutoDrawable drawable) {
	}

	public void init(GLAutoDrawable drawable) {
		gl = drawable.getGL().getGL2();
		for (int iFin = 0; iFin < 5; iFin++) {
			for (int iBon = 0; iBon < 5; iBon++) {
				for (int xyz = 0; xyz < 3; xyz++) {
					PosNow[iBon][iFin][xyz] = new Double(0);
					if (iBon <= 3) {
						dirNow[iBon][iFin][xyz] = new Double(0);
						dirBefInterval[iBon][iFin][xyz] = new Double(0);
					}
				}
			}
		}
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
		float ratio = (float) h / (float) w;
		gl.glViewport(0, 0, w, h);
		gl.glMatrixMode(GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glFrustum(-1.0f, 1.0f, -ratio, ratio, 5.0f, 40.0f);
		gl.glMatrixMode(GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glTranslatef(0.0f, 0.0f, -20.0f);
	}
}