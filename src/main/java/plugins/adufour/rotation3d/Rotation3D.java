package plugins.adufour.rotation3d;

import icy.canvas.IcyCanvas;
import icy.gui.viewer.Viewer;
import icy.sequence.Sequence;
import icy.system.IcyHandledException;
import plugins.adufour.ezplug.EzLabel;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzStoppable;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.kernel.canvas.VtkCanvas;

public class Rotation3D extends EzPlug implements EzStoppable
{
    private EzVarInteger stepAngle;

    @Override
    protected void initialize()
    {
        addEzComponent(new EzLabel("Rotation is achieved about the vertical axis"));
        addEzComponent(stepAngle = new EzVarInteger("Angle step", 5, 1, 90, 1));
        stepAngle.setToolTipText("Lower step = slower, smoother rotation");
    }

    @Override
    public void execute()
    {
        Viewer activeViewer = getActiveViewer();

        if (activeViewer == null)
            throw new IcyHandledException("No sequence opened.");

        IcyCanvas canvas = activeViewer.getCanvas();

        if (!(canvas instanceof VtkCanvas))
            throw new IcyHandledException("Rotation only works on a 3D (VTK) view.");

        activeViewer.toFront();

        final Sequence rotatedView = new Sequence("3D rotated view of " + getActiveSequence().getName());
        final VtkCanvas c3d = (VtkCanvas) canvas;
        final int sizeT = c3d.getImageSizeT();

        final int step = stepAngle.getValue().intValue();
        if ((step <= 0) || (step > 360))
            throw new IcyHandledException("Angle should be in [1..360] range");

        final int n = 360 / step;
        final double stepT = (double) sizeT / (double) n;

        // Take one image for nothing (to minimize double-buffering artifacts)
        c3d.getRenderedImage(0, 0);

        double t = 0d;
        for (int i = 0; i < n; i++)
        {
            final int posT = (int) Math.floor(t);
            
            getUI().setProgressBarValue(i / (double) n);
            getUI().setProgressBarMessage("capturing angle " + i + "/" + n);

            c3d.setPositionT(posT);
            c3d.getVtkPanel().rotateView(step, 0);

            rotatedView.setImage(i, 0, c3d.getRenderedImage(posT, 0));
            t += stepT;

            if (Thread.currentThread().isInterrupted())
                break;
        }

        addSequence(rotatedView);
    }

    @Override
    public void clean()
    {

    }
}
