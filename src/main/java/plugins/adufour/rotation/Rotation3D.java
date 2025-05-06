/*
 * Copyright (c) 2010-2025. Institut Pasteur.
 *
 * This file is part of Icy.
 * Icy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Icy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Icy. If not, see <https://www.gnu.org/licenses/>.
 */

package plugins.adufour.rotation;

import org.bioimageanalysis.icy.extension.plugin.annotation_.IcyPluginIcon;
import org.bioimageanalysis.icy.extension.plugin.annotation_.IcyPluginName;
import org.bioimageanalysis.icy.gui.canvas.IcyCanvas;
import org.bioimageanalysis.icy.gui.canvas.VtkCanvas;
import org.bioimageanalysis.icy.gui.viewer.Viewer;
import org.bioimageanalysis.icy.model.sequence.Sequence;
import org.bioimageanalysis.icy.system.IcyHandledException;
import plugins.adufour.ezplug.EzLabel;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzStoppable;
import plugins.adufour.ezplug.EzVarInteger;

@IcyPluginName("3D Rotation")
@IcyPluginIcon(path = "/plugins/adufour/rotation/3d-rotation.png")
public class Rotation3D extends EzPlug implements EzStoppable {
    private EzVarInteger stepAngle;

    @Override
    protected void initialize() {
        addEzComponent(new EzLabel("Rotation is achieved about the vertical axis"));
        addEzComponent(stepAngle = new EzVarInteger("Angle step", 5, 1, 90, 1));
        stepAngle.setToolTipText("Lower step = slower, smoother rotation");
    }

    @Override
    public void execute() {
        final Viewer activeViewer = getActiveViewer();

        if (activeViewer == null)
            throw new IcyHandledException("No sequence opened.");

        final IcyCanvas canvas = activeViewer.getCanvas();

        if (!(canvas instanceof final VtkCanvas c3d))
            throw new IcyHandledException("Rotation only works on a 3D (VTK) view.");

        activeViewer.toFront();

        final Sequence rotatedView = new Sequence("3D rotated view of " + getActiveSequence().getName());
        final int sizeT = c3d.getImageSizeT();

        final int step = stepAngle.getValue();
        if ((step <= 0) || (step > 360))
            throw new IcyHandledException("Angle should be in [1..360] range");

        final int n = 360 / step;
        final double stepT = (double) sizeT / (double) n;

        // Take one image for nothing (to minimize double-buffering artifacts)
        c3d.getRenderedImage(0, 0);

        double t = 0d;
        for (int i = 0; i < n; i++) {
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
    public void clean() {

    }
}
