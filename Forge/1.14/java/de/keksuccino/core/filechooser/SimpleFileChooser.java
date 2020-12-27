
package de.keksuccino.core.filechooser;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class SimpleFileChooser extends JFileChooser {

	private static final long serialVersionUID = 3495748029055740354L;
	
	private JDialog dialog = null;
	private File homeDir;
	
	public SimpleFileChooser(File homeDir) {
		this.homeDir = homeDir;
		this.setCurrentDirectory(this.homeDir);
	}
	
	@Override
	protected JDialog createDialog(Component parent) throws HeadlessException {
		this.dialog = super.createDialog(parent);

		//Hide top directory/drive navigator and replace "home" and "got to parent dir" buttons
		int i = 0;
		for (Component c : ((JPanel)this.getComponents()[0]).getComponents()) {
			if (c instanceof JLabel) {
				c.setVisible(false);
			}
			if (c.getClass().getName().startsWith("javax.swing.plaf.metal.MetalFileChooserUI")) {
				c.setVisible(false);
			}

			if (c instanceof JPanel) {
				List<JToggleButton> toggles = new ArrayList<JToggleButton>();
				JButton newFolder = null;
				Icon home = null;
				Icon up = null;
				
				for (Component cc : ((Container)c).getComponents()) {
					if (cc instanceof JButton) {
						if (i == 0) {
							up = ((JButton)cc).getIcon();
							cc.setVisible(false);
						}
						if (i == 1) {
							home = ((JButton)cc).getIcon();
							cc.setVisible(false);
						}
						if (i == 2) {
							newFolder = (JButton)cc;
						}
						i++;
					}
					
					if (cc instanceof JToggleButton) {
						toggles.add((JToggleButton)cc);
					}
				}

				JButton homeButton = new JButton();
				homeButton.setMaximumSize(new Dimension(50, 24));
				homeButton.setIcon(home);
				homeButton.addMouseListener(new MouseListener() {
					@Override
					public void mouseReleased(MouseEvent e) {
					}
					@Override
					public void mousePressed(MouseEvent e) {
					}
					@Override
					public void mouseExited(MouseEvent e) {
					}
					@Override
					public void mouseEntered(MouseEvent e) {
					}
					@Override
					public void mouseClicked(MouseEvent e) {
						SimpleFileChooser.this.setCurrentDirectory(SimpleFileChooser.this.homeDir);
					}
				});
				((JPanel)c).add(homeButton);
				
				JButton upButton = new JButton();
				upButton.setMaximumSize(new Dimension(50, 24));
				upButton.setIcon(up);
				upButton.addMouseListener(new MouseListener() {
					@Override
					public void mouseReleased(MouseEvent e) {
					}
					@Override
					public void mousePressed(MouseEvent e) {
					}
					@Override
					public void mouseExited(MouseEvent e) {
					}
					@Override
					public void mouseEntered(MouseEvent e) {
					}
					@Override
					public void mouseClicked(MouseEvent e) {
						File f = SimpleFileChooser.this.getCurrentDirectory().getParentFile();
						if ((f != null) && f.getAbsolutePath().startsWith(SimpleFileChooser.this.homeDir.getAbsolutePath())) {
							SimpleFileChooser.this.setCurrentDirectory(f);
						}
					}
				});
				((JPanel)c).add(upButton);
				
				((JPanel)c).remove(newFolder);
				((JPanel)c).add(newFolder);
				
				for (JToggleButton b : toggles) {
					((JPanel)c).remove(b);
					((JPanel)c).add(b);
				}
			}
		}
		
		//Hide all bottom controls except of "Cancel" and "Open"
		for (Component c : ((JPanel)this.getComponents()[3]).getComponents()) {
			for (Component cc : ((Container)c).getComponents()) {
				if (!(cc instanceof JButton)) {
					cc.setVisible(false);
				}
			}
		}
		
		this.dialog.setAlwaysOnTop(true);
		
		return this.dialog;
	}

}
