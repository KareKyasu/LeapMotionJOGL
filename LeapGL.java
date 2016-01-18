package leapMotionJOGL;

import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;

import com.jogamp.opengl.util.Animator;
import com.leapmotion.leap.Bone;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Finger;
import com.leapmotion.leap.Hand;

class LeapGL implements GLEventListener {
	private GL2 gl;
	static int SizeX = 1000, SizeY = 500;
	static java.awt.Frame frame;
	static Controller controller;
	static com.leapmotion.leap.Frame leapFrame;
	static LeapGL leapGL;

	public static void main(String[] args) {
		controller = new Controller();
		leapFrame = controller.frame();
		frame = new java.awt.Frame("LeapGL");
		GLCanvas canvas = new GLCanvas();
		leapGL = new LeapGL();
		canvas.addGLEventListener(leapGL);
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

	public LeapGL() {
	}

	public void display(GLAutoDrawable drawable) {
		gl = drawable.getGL().getGL2();
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		gl.glPushMatrix();
		drawTwoHands(gl);
		gl.glPopMatrix();
	}

	void drawTwoHands(GL2 gl) {
		leapFrame = controller.frame();
		for (Hand hand : leapFrame.hands()) {
			for (Finger finger : hand.fingers()) {
				gl.glColor3f(0.4f, 0.8f, 0.2f);
				gl.glBegin(3);
				for (Bone.Type boneType : Bone.Type.values()) {
					Bone bone = finger.bone(boneType);
					gl.glVertex3d(bone.prevJoint().getX() / 50.0d, (bone
							.prevJoint().getY() - (SizeY / 2)) / 100.0d, bone
							.prevJoint().getZ() / 50.0d);
				}
				gl.glEnd();
				gl.glColor3f(0.1f, 0.1f, 1.0f);
				gl.glBegin(0);
				for (Bone.Type boneType : Bone.Type.values()) {
					Bone bone = finger.bone(boneType);
					gl.glVertex3d(bone.prevJoint().getX() / 50.0d, (bone
							.prevJoint().getY() - (SizeY / 2)) / 100.0d, bone
							.prevJoint().getZ() / 50.0d);
				}
				gl.glEnd();
			}
		}
	}

	public void dispose(GLAutoDrawable drawable) {
	}

	public void init(GLAutoDrawable drawable) {
		gl = drawable.getGL().getGL2();
		gl.glLineWidth(4.0f);
		gl.glPointSize(5f);
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