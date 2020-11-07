package test.filter.cam.filter;
import android.content.Context;
import android.opengl.GLES20;

import test.filter.cam.MyGLUtils;
import test.filter.cam.R;


public class EdgeDetectionFilter extends CameraFilter {
    private int program;

    public EdgeDetectionFilter(Context context) {
        super(context);

        // Build shaders
        program = MyGLUtils.buildProgram(context, R.raw.vertext, R.raw.edge_detection);
    }

    @Override
    public void onDraw(int cameraTexId, int canvasWidth, int canvasHeight) {
        setupShaderInputs(program,
                new int[]{canvasWidth, canvasHeight},
                new int[]{cameraTexId},
                new int[][]{});
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }
}
