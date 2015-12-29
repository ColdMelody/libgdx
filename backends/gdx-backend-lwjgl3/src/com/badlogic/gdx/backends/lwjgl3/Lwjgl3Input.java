package com.badlogic.gdx.backends.lwjgl3;

import static org.lwjgl.glfw.GLFW.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputEventQueue;
import com.badlogic.gdx.InputProcessor;

public class Lwjgl3Input implements Input {
	private final Lwjgl3Window window;
	private InputProcessor inputProcessor;
	private final InputEventQueue eventQueue = new InputEventQueue();
	
	private int mouseX, mouseY;
	private int mousePressed;
	private int deltaX, deltaY;
	private boolean justTouched;
	private int pressedKeys;
	private boolean keyJustPressed;
	private boolean[] justPressedKeys = new boolean[256];
	private long currentEventTime;	
	private char lastCharacter;
	
	private GLFWKeyCallback keyCallback = new GLFWKeyCallback() {		
		@Override
		public void invoke(long window, int key, int scancode, int action, int mods) {
			switch (action) {
			case GLFW_PRESS:
				key = getGdxKeyCode(key);
				eventQueue.keyDown(key);
				pressedKeys++;
				keyJustPressed = true;
				justPressedKeys[key] = true;
				Lwjgl3Input.this.window.getGraphics().requestRendering();
				lastCharacter = 0;
				char character = characterForKeyCode(key);
				if (character != 0) charCallback.invoke(window, character);				
				break;
			case GLFW_RELEASE:
				pressedKeys--;
				Lwjgl3Input.this.window.getGraphics().requestRendering();
				eventQueue.keyUp(getGdxKeyCode(key));
				break;

			case GLFW_REPEAT:
				if (lastCharacter != 0) {
					Lwjgl3Input.this.window.getGraphics().requestRendering();
					eventQueue.keyTyped(lastCharacter);
				}
				break;
			}
		}
	};
	
	private GLFWCharCallback charCallback = new GLFWCharCallback() {
		@Override
		public void invoke(long window, int codepoint) {
			if ((codepoint & 0xff00) == 0xf700) return;
			lastCharacter = (char)codepoint;
			Lwjgl3Input.this.window.getGraphics().requestRendering();
			eventQueue.keyTyped((char)codepoint);
		}
	};
	
	private GLFWScrollCallback scrollCallback = new GLFWScrollCallback() {
		@Override
		public void invoke(long window, double scrollX, double scrollY) {
			Lwjgl3Input.this.window.getGraphics().requestRendering();
			eventQueue.scrolled((int)-Math.signum(scrollY));
		}
	};
	
	private GLFWCursorPosCallback cursorPosCallback = new GLFWCursorPosCallback() {
		@Override
		public void invoke(long windowHandle, double x, double y) {
			deltaX = (int)x - mouseX;
			deltaY = (int)y - mouseY;
			mouseX = (int)x;
			mouseY = (int)y;
			
			if(window.getConfig().useHDPI) {
				float xScale = window.getGraphics().getFrameBufferWidth() / (float)window.getGraphics().getLogicalWidth();
				float yScale = window.getGraphics().getFrameBufferHeight() / (float)window.getGraphics().getLogicalHeight();				
				deltaX = (int)(deltaX * xScale);
				deltaY = (int)(deltaY * yScale);
				mouseX = (int)(mouseX * xScale);
				mouseY = (int)(mouseY * yScale);
			}
			
			Lwjgl3Input.this.window.getGraphics().requestRendering();
			if (mousePressed > 0) {								
				eventQueue.touchDragged(mouseX, mouseY, 0);
			} else {								
				eventQueue.mouseMoved(mouseX, mouseY);
			}			
		}
	};
	
	private GLFWMouseButtonCallback mouseButtonCallback = new GLFWMouseButtonCallback() {
		@Override
		public void invoke(long window, int button, int action, int mods) {
			int gdxButton = toGdxButton(button);
			if (button != -1 && gdxButton == -1) return;

			if (action == GLFW_PRESS) {
				mousePressed++;
				justTouched = true;
				Lwjgl3Input.this.window.getGraphics().requestRendering();
				eventQueue.touchDown(mouseX, mouseY, 0, gdxButton);
			} else {
				mousePressed = Math.max(0, mousePressed - 1);
				Lwjgl3Input.this.window.getGraphics().requestRendering();
				eventQueue.touchUp(mouseX, mouseY, 0, gdxButton);
			}
		}
		
		private int toGdxButton (int button) {
			if (button == 0) return Buttons.LEFT;
			if (button == 1) return Buttons.RIGHT;
			if (button == 2) return Buttons.MIDDLE;
			if (button == 3) return Buttons.BACK;
			if (button == 4) return Buttons.FORWARD;
			return -1;
		}
	};
	
	public Lwjgl3Input(Lwjgl3Window window) {
		this.window = window;
		windowHandleChanged(window.getWindowHandle());
	}	
	
	
	public void windowHandleChanged(long windowHandle) {		
		glfwSetKeyCallback(window.getWindowHandle(), keyCallback);
		glfwSetCharCallback(window.getWindowHandle(), charCallback);
		glfwSetScrollCallback(window.getWindowHandle(), scrollCallback);
		glfwSetCursorPosCallback(window.getWindowHandle(), cursorPosCallback);
		glfwSetMouseButtonCallback(window.getWindowHandle(), mouseButtonCallback);
	}	
	
	public void update() {
		deltaX = 0;
		deltaY = 0;
		justTouched = false;
		
		if (keyJustPressed) {
			keyJustPressed = false;
			for (int i = 0; i < justPressedKeys.length; i++) {
				justPressedKeys[i] = false;
			}
		}		
		eventQueue.setProcessor(inputProcessor);
		eventQueue.drain();
	}	

	@Override
	public int getX() {
		return mouseX;
	}

	@Override
	public int getX(int pointer) {
		return pointer == 0? mouseX: 0;
	}

	@Override
	public int getDeltaX() {
		return deltaX;
	}

	@Override
	public int getDeltaX(int pointer) {
		return pointer == 0? deltaX: 0;
	}

	@Override
	public int getY() {
		return mouseY;
	}

	@Override
	public int getY(int pointer) {
		return pointer == 0? mouseY: 0;
	}

	@Override
	public int getDeltaY() {
		return deltaY;
	}

	@Override
	public int getDeltaY(int pointer) {
		return pointer == 0? deltaY: 0;
	}

	@Override
	public boolean isTouched() {
		return GLFW.glfwGetMouseButton(window.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS ||
				GLFW.glfwGetMouseButton(window.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_2) == GLFW.GLFW_PRESS ||
				GLFW.glfwGetMouseButton(window.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_3) == GLFW.GLFW_PRESS ||
				GLFW.glfwGetMouseButton(window.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_4) == GLFW.GLFW_PRESS ||
				GLFW.glfwGetMouseButton(window.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_5) == GLFW.GLFW_PRESS;
	}

	@Override
	public boolean justTouched() {
		return justTouched;
	}

	@Override
	public boolean isTouched(int pointer) {
		return pointer == 0? isTouched(): false;
	}

	@Override
	public boolean isButtonPressed(int button) {
		return GLFW.glfwGetMouseButton(window.getWindowHandle(), button) == GLFW.GLFW_PRESS;
	}

	@Override
	public boolean isKeyPressed(int key) {
		if (key == Input.Keys.ANY_KEY) return pressedKeys > 0;
		if (key == Input.Keys.SYM) {					
			return GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_LEFT_SUPER) == GLFW.GLFW_PRESS|| 
					GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_RIGHT_SUPER) == GLFW.GLFW_PRESS;
		}
		return GLFW.glfwGetKey(window.getWindowHandle(), getGlfwKeyCode(key)) == GLFW.GLFW_PRESS;
	}

	@Override
	public boolean isKeyJustPressed(int key) {
		if (key == Input.Keys.ANY_KEY) {
			return keyJustPressed;
		}
		if (key < 0 || key > 256) {
			return false;
		}
		return justPressedKeys[key];
	}

	@Override
	public void getTextInput(TextInputListener listener, String title, String text, String hint) {
		// FIXME
	}

	@Override
	public long getCurrentEventTime() {
		return currentEventTime;
	}	

	@Override
	public void setInputProcessor(InputProcessor processor) {
		this.inputProcessor = processor;
	}

	@Override
	public InputProcessor getInputProcessor() {
		return inputProcessor;
	}
	
	@Override
	public void setCursorCatched(boolean catched) {
		glfwSetInputMode(window.getWindowHandle(), GLFW.GLFW_CURSOR, catched ? GLFW.GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
	}

	@Override
	public boolean isCursorCatched() {
		return glfwGetInputMode(window.getWindowHandle(), GLFW_CURSOR) == GLFW_CURSOR_DISABLED;
	}

	@Override
	public void setCursorPosition(int x, int y) {
		if(window.getConfig().useHDPI) {
			float xScale = window.getGraphics().getLogicalWidth() / (float)window.getGraphics().getFrameBufferWidth();
			float yScale = window.getGraphics().getLogicalHeight() / (float)window.getGraphics().getFrameBufferHeight();
			x = (int)(x * xScale);
			y = (int)(y * yScale);
		}
		glfwSetCursorPos(window.getWindowHandle(), x, y);		
	}
	
	static char characterForKeyCode (int key) {
		// Map certain key codes to character codes.
		switch (key) {
		case Keys.BACKSPACE:
			return 8;
		case Keys.TAB:
			return '\t';
		case Keys.FORWARD_DEL:
			return 127;
		}
		return 0;
	}

	static public int getGdxKeyCode (int lwjglKeyCode) {
		switch (lwjglKeyCode) {
		case GLFW_KEY_SPACE:
			return Input.Keys.SPACE;
		case GLFW_KEY_APOSTROPHE:
			return Input.Keys.APOSTROPHE;
		case GLFW_KEY_COMMA:
			return Input.Keys.COMMA;
		case GLFW_KEY_MINUS:
			return Input.Keys.MINUS;
		case GLFW_KEY_PERIOD:
			return Input.Keys.PERIOD;
		case GLFW_KEY_SLASH:
			return Input.Keys.SLASH;
		case GLFW_KEY_0:
			return Input.Keys.NUM_0;
		case GLFW_KEY_1:
			return Input.Keys.NUM_1;
		case GLFW_KEY_2:
			return Input.Keys.NUM_2;
		case GLFW_KEY_3:
			return Input.Keys.NUM_3;
		case GLFW_KEY_4:
			return Input.Keys.NUM_4;
		case GLFW_KEY_5:
			return Input.Keys.NUM_5;
		case GLFW_KEY_6:
			return Input.Keys.NUM_6;
		case GLFW_KEY_7:
			return Input.Keys.NUM_7;
		case GLFW_KEY_8:
			return Input.Keys.NUM_8;
		case GLFW_KEY_9:
			return Input.Keys.NUM_9;
		case GLFW_KEY_SEMICOLON:
			return Input.Keys.SEMICOLON;
		case GLFW_KEY_EQUAL:
			return Input.Keys.EQUALS;
		case GLFW_KEY_A:
			return Input.Keys.A;
		case GLFW_KEY_B:
			return Input.Keys.B;
		case GLFW_KEY_C:
			return Input.Keys.C;
		case GLFW_KEY_D:
			return Input.Keys.D;
		case GLFW_KEY_E:
			return Input.Keys.E;
		case GLFW_KEY_F:
			return Input.Keys.F;
		case GLFW_KEY_G:
			return Input.Keys.G;
		case GLFW_KEY_H:
			return Input.Keys.H;
		case GLFW_KEY_I:
			return Input.Keys.I;
		case GLFW_KEY_J:
			return Input.Keys.J;
		case GLFW_KEY_K:
			return Input.Keys.K;
		case GLFW_KEY_L:
			return Input.Keys.L;
		case GLFW_KEY_M:
			return Input.Keys.M;
		case GLFW_KEY_N:
			return Input.Keys.N;
		case GLFW_KEY_O:
			return Input.Keys.O;
		case GLFW_KEY_P:
			return Input.Keys.P;
		case GLFW_KEY_Q:
			return Input.Keys.Q;
		case GLFW_KEY_R:
			return Input.Keys.R;
		case GLFW_KEY_S:
			return Input.Keys.S;
		case GLFW_KEY_T:
			return Input.Keys.T;
		case GLFW_KEY_U:
			return Input.Keys.U;
		case GLFW_KEY_V:
			return Input.Keys.V;
		case GLFW_KEY_W:
			return Input.Keys.W;
		case GLFW_KEY_X:
			return Input.Keys.X;
		case GLFW_KEY_Y:
			return Input.Keys.Y;
		case GLFW_KEY_Z:
			return Input.Keys.Z;
		case GLFW_KEY_LEFT_BRACKET:
			return Input.Keys.LEFT_BRACKET;
		case GLFW_KEY_BACKSLASH:
			return Input.Keys.BACKSLASH;
		case GLFW_KEY_RIGHT_BRACKET:
			return Input.Keys.RIGHT_BRACKET;
		case GLFW_KEY_GRAVE_ACCENT:
			return Input.Keys.GRAVE;
		case GLFW_KEY_WORLD_1:
		case GLFW_KEY_WORLD_2:
			return Input.Keys.UNKNOWN;
		case GLFW_KEY_ESCAPE:
			return Input.Keys.ESCAPE;
		case GLFW_KEY_ENTER:
			return Input.Keys.ENTER;
		case GLFW_KEY_TAB:
			return Input.Keys.TAB;
		case GLFW_KEY_BACKSPACE:
			return Input.Keys.BACKSPACE;
		case GLFW_KEY_INSERT:
			return Input.Keys.INSERT;
		case GLFW_KEY_DELETE:
			return Input.Keys.FORWARD_DEL;
		case GLFW_KEY_RIGHT:
			return Input.Keys.RIGHT;
		case GLFW_KEY_LEFT:
			return Input.Keys.LEFT;
		case GLFW_KEY_DOWN:
			return Input.Keys.DOWN;
		case GLFW_KEY_UP:
			return Input.Keys.UP;
		case GLFW_KEY_PAGE_UP:
			return Input.Keys.PAGE_UP;
		case GLFW_KEY_PAGE_DOWN:
			return Input.Keys.PAGE_DOWN;
		case GLFW_KEY_HOME:
			return Input.Keys.HOME;
		case GLFW_KEY_END:
			return Input.Keys.END;
		case GLFW_KEY_CAPS_LOCK:
		case GLFW_KEY_SCROLL_LOCK:
		case GLFW_KEY_NUM_LOCK:
		case GLFW_KEY_PRINT_SCREEN:
		case GLFW_KEY_PAUSE:
			return Input.Keys.UNKNOWN;
		case GLFW_KEY_F1:
			return Input.Keys.F1;
		case GLFW_KEY_F2:
			return Input.Keys.F2;
		case GLFW_KEY_F3:
			return Input.Keys.F3;
		case GLFW_KEY_F4:
			return Input.Keys.F4;
		case GLFW_KEY_F5:
			return Input.Keys.F5;
		case GLFW_KEY_F6:
			return Input.Keys.F6;
		case GLFW_KEY_F7:
			return Input.Keys.F7;
		case GLFW_KEY_F8:
			return Input.Keys.F8;
		case GLFW_KEY_F9:
			return Input.Keys.F9;
		case GLFW_KEY_F10:
			return Input.Keys.F10;
		case GLFW_KEY_F11:
			return Input.Keys.F11;
		case GLFW_KEY_F12:
			return Input.Keys.F12;
		case GLFW_KEY_F13:
		case GLFW_KEY_F14:
		case GLFW_KEY_F15:
		case GLFW_KEY_F16:
		case GLFW_KEY_F17:
		case GLFW_KEY_F18:
		case GLFW_KEY_F19:
		case GLFW_KEY_F20:
		case GLFW_KEY_F21:
		case GLFW_KEY_F22:
		case GLFW_KEY_F23:
		case GLFW_KEY_F24:
		case GLFW_KEY_F25:
			return Input.Keys.UNKNOWN;
		case GLFW_KEY_KP_0:
			return Input.Keys.NUMPAD_0;
		case GLFW_KEY_KP_1:
			return Input.Keys.NUMPAD_1;
		case GLFW_KEY_KP_2:
			return Input.Keys.NUMPAD_2;
		case GLFW_KEY_KP_3:
			return Input.Keys.NUMPAD_3;
		case GLFW_KEY_KP_4:
			return Input.Keys.NUMPAD_4;
		case GLFW_KEY_KP_5:
			return Input.Keys.NUMPAD_5;
		case GLFW_KEY_KP_6:
			return Input.Keys.NUMPAD_6;
		case GLFW_KEY_KP_7:
			return Input.Keys.NUMPAD_7;
		case GLFW_KEY_KP_8:
			return Input.Keys.NUMPAD_8;
		case GLFW_KEY_KP_9:
			return Input.Keys.NUMPAD_9;
		case GLFW_KEY_KP_DECIMAL:
			return Input.Keys.PERIOD;
		case GLFW_KEY_KP_DIVIDE:
			return Input.Keys.SLASH;
		case GLFW_KEY_KP_MULTIPLY:
			return Input.Keys.STAR;
		case GLFW_KEY_KP_SUBTRACT:
			return Input.Keys.MINUS;
		case GLFW_KEY_KP_ADD:
			return Input.Keys.PLUS;
		case GLFW_KEY_KP_ENTER:
			return Input.Keys.ENTER;
		case GLFW_KEY_KP_EQUAL:
			return Input.Keys.EQUALS;
		case GLFW_KEY_LEFT_SHIFT:
			return Input.Keys.SHIFT_LEFT;
		case GLFW_KEY_LEFT_CONTROL:
			return Input.Keys.CONTROL_LEFT;
		case GLFW_KEY_LEFT_ALT:
			return Input.Keys.ALT_LEFT;
		case GLFW_KEY_LEFT_SUPER:
			return Input.Keys.SYM;
		case GLFW_KEY_RIGHT_SHIFT:
			return Input.Keys.SHIFT_RIGHT;
		case GLFW_KEY_RIGHT_CONTROL:
			return Input.Keys.CONTROL_RIGHT;
		case GLFW_KEY_RIGHT_ALT:
			return Input.Keys.ALT_RIGHT;
		case GLFW_KEY_RIGHT_SUPER:
			return Input.Keys.SYM;
		case GLFW_KEY_MENU:
			return Input.Keys.MENU;
		default:
			return Input.Keys.UNKNOWN;
		}
	}
	
	static public int getGlfwKeyCode (int gdxKeyCode) {
		switch (gdxKeyCode) {
		case Input.Keys.SPACE:
			return GLFW_KEY_SPACE;
		case Input.Keys.APOSTROPHE:
			return GLFW_KEY_APOSTROPHE;
		case Input.Keys.COMMA:
			return GLFW_KEY_COMMA;
		case Input.Keys.PERIOD:
			return GLFW_KEY_PERIOD;
		case Input.Keys.NUM_0:
			return GLFW_KEY_0;
		case Input.Keys.NUM_1:
			return GLFW_KEY_1;
		case Input.Keys.NUM_2:
			return GLFW_KEY_2;
		case Input.Keys.NUM_3:
			return GLFW_KEY_3;
		case Input.Keys.NUM_4:
			return GLFW_KEY_4;
		case Input.Keys.NUM_5:
			return GLFW_KEY_5;
		case Input.Keys.NUM_6:
			return GLFW_KEY_6;
		case Input.Keys.NUM_7:
			return GLFW_KEY_7;
		case Input.Keys.NUM_8:
			return GLFW_KEY_8;
		case Input.Keys.NUM_9:
			return GLFW_KEY_9;
		case Input.Keys.SEMICOLON:
			return GLFW_KEY_SEMICOLON;
		case Input.Keys.EQUALS:
			return GLFW_KEY_EQUAL;
		case Input.Keys.A:
			return GLFW_KEY_A;
		case Input.Keys.B:
			return GLFW_KEY_B;
		case Input.Keys.C:
			return GLFW_KEY_C;
		case Input.Keys.D:
			return GLFW_KEY_D;
		case Input.Keys.E:
			return GLFW_KEY_E;
		case Input.Keys.F:
			return GLFW_KEY_F;
		case Input.Keys.G:
			return GLFW_KEY_G;
		case Input.Keys.H:
			return GLFW_KEY_H;
		case Input.Keys.I:
			return GLFW_KEY_I;
		case Input.Keys.J:
			return GLFW_KEY_J;
		case Input.Keys.K:
			return GLFW_KEY_K;
		case Input.Keys.L:
			return GLFW_KEY_L;
		case Input.Keys.M:
			return GLFW_KEY_M;
		case Input.Keys.N:
			return GLFW_KEY_N;
		case Input.Keys.O:
			return GLFW_KEY_O;
		case Input.Keys.P:
			return GLFW_KEY_P;
		case Input.Keys.Q:
			return GLFW_KEY_Q;
		case Input.Keys.R:
			return GLFW_KEY_R;
		case Input.Keys.S:
			return GLFW_KEY_S;
		case Input.Keys.T:
			return GLFW_KEY_T;
		case Input.Keys.U:
			return GLFW_KEY_U;
		case Input.Keys.V:
			return GLFW_KEY_V;
		case Input.Keys.W:
			return GLFW_KEY_W;
		case Input.Keys.X:
			return GLFW_KEY_X;
		case Input.Keys.Y:
			return GLFW_KEY_Y;
		case Input.Keys.Z:
			return GLFW_KEY_Z;
		case Input.Keys.LEFT_BRACKET:
			return GLFW_KEY_LEFT_BRACKET;
		case Input.Keys.BACKSLASH:
			return GLFW_KEY_BACKSLASH;
		case Input.Keys.RIGHT_BRACKET:
			return GLFW_KEY_RIGHT_BRACKET;
		case Input.Keys.GRAVE:
			return GLFW_KEY_GRAVE_ACCENT;
		case Input.Keys.ESCAPE:
			return GLFW_KEY_ESCAPE;
		case Input.Keys.ENTER:
			return GLFW_KEY_ENTER;
		case Input.Keys.TAB:
			return GLFW_KEY_TAB;
		case Input.Keys.BACKSPACE:
			return GLFW_KEY_BACKSPACE;
		case Input.Keys.INSERT:
			return GLFW_KEY_INSERT;
		case Input.Keys.FORWARD_DEL:
			return GLFW_KEY_DELETE;
		case Input.Keys.RIGHT:
			return GLFW_KEY_RIGHT;
		case Input.Keys.LEFT:
			return GLFW_KEY_LEFT;
		case Input.Keys.DOWN:
			return GLFW_KEY_DOWN;
		case Input.Keys.UP:
			return GLFW_KEY_UP;
		case Input.Keys.PAGE_UP:
			return GLFW_KEY_PAGE_UP;
		case Input.Keys.PAGE_DOWN:
			return GLFW_KEY_PAGE_DOWN;
		case Input.Keys.HOME:
			return GLFW_KEY_HOME;
		case Input.Keys.END:
			return GLFW_KEY_END;
		case Input.Keys.F1:
			return GLFW_KEY_F1;
		case Input.Keys.F2:
			return GLFW_KEY_F2;
		case Input.Keys.F3:
			return GLFW_KEY_F3;
		case Input.Keys.F4:
			return GLFW_KEY_F4;
		case Input.Keys.F5:
			return GLFW_KEY_F5;
		case Input.Keys.F6:
			return GLFW_KEY_F6;
		case Input.Keys.F7:
			return GLFW_KEY_F7;
		case Input.Keys.F8:
			return GLFW_KEY_F8;
		case Input.Keys.F9:
			return GLFW_KEY_F9;
		case Input.Keys.F10:
			return GLFW_KEY_F10;
		case Input.Keys.F11:
			return GLFW_KEY_F11;
		case Input.Keys.F12:
			return GLFW_KEY_F12;
		case Input.Keys.NUMPAD_0:
			return GLFW_KEY_KP_0;
		case Input.Keys.NUMPAD_1:
			return GLFW_KEY_KP_1;
		case Input.Keys.NUMPAD_2:
			return GLFW_KEY_KP_2;
		case Input.Keys.NUMPAD_3:
			return GLFW_KEY_KP_3;
		case Input.Keys.NUMPAD_4:
			return GLFW_KEY_KP_4;
		case Input.Keys.NUMPAD_5:
			return GLFW_KEY_KP_5;
		case Input.Keys.NUMPAD_6:
			return GLFW_KEY_KP_6;
		case Input.Keys.NUMPAD_7:
			return GLFW_KEY_KP_7;
		case Input.Keys.NUMPAD_8:
			return GLFW_KEY_KP_8;
		case Input.Keys.NUMPAD_9:
			return GLFW_KEY_KP_9;
		case Input.Keys.SLASH:
			return GLFW_KEY_KP_DIVIDE;
		case Input.Keys.STAR:
			return GLFW_KEY_KP_MULTIPLY;
		case Input.Keys.MINUS:
			return GLFW_KEY_KP_SUBTRACT;
		case Input.Keys.PLUS:
			return GLFW_KEY_KP_ADD;
		case Input.Keys.SHIFT_LEFT:
			return GLFW_KEY_LEFT_SHIFT;
		case Input.Keys.CONTROL_LEFT:
			return GLFW_KEY_LEFT_CONTROL;
		case Input.Keys.ALT_LEFT:
			return GLFW_KEY_LEFT_ALT;
		case Input.Keys.SYM:
			return GLFW_KEY_LEFT_SUPER;
		case Input.Keys.SHIFT_RIGHT:
			return GLFW_KEY_RIGHT_SHIFT;
		case Input.Keys.CONTROL_RIGHT:
			return GLFW_KEY_RIGHT_CONTROL;
		case Input.Keys.ALT_RIGHT:
			return GLFW_KEY_RIGHT_ALT;
		case Input.Keys.MENU:
			return GLFW_KEY_MENU;
		default:
			return 0;
		}
	}

	// --------------------------------------------------------------------------
	// -------------------------- Nothing to see below this line except for stubs
	// --------------------------------------------------------------------------
	@Override
	public void setCatchBackKey(boolean catchBack) {
	}

	@Override
	public boolean isCatchBackKey() {
		return false;
	}

	@Override
	public void setCatchMenuKey(boolean catchMenu) {
	}

	@Override
	public boolean isCatchMenuKey() {
		return false;
	}
	
	@Override
	public float getAccelerometerX() {
		return 0;
	}

	@Override
	public float getAccelerometerY() {
		return 0;
	}

	@Override
	public float getAccelerometerZ() {
		return 0;
	}
	
	@Override
	public boolean isPeripheralAvailable(Peripheral peripheral) {
		return peripheral == Peripheral.HardwareKeyboard;
	}

	@Override
	public int getRotation() {
		return 0;
	}

	@Override
	public Orientation getNativeOrientation() {
		return Orientation.Landscape;
	}
	
	@Override
	public void setOnscreenKeyboardVisible(boolean visible) {
	}

	@Override
	public void vibrate(int milliseconds) {
	}

	@Override
	public void vibrate(long[] pattern, int repeat) {
	}

	@Override
	public void cancelVibrate() {
	}

	@Override
	public float getAzimuth() {
		return 0;
	}

	@Override
	public float getPitch() {
		return 0;
	}

	@Override
	public float getRoll() {
		return 0;
	}

	@Override
	public void getRotationMatrix(float[] matrix) {
	}

}
