package video;

import ij.*;
import ij.gui.*;
import ij.plugin.frame.*;
import ij.plugin.*;
import ij.process.*;
import ij.io.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;


/**
 *  About Window
 *
 *@author     thomas dot boudier at snv dot jussieu dot fr
 *@created    26 decembre 2009
 */
class AboutWindowVideoJ extends JFrame {

	/**
	 *  Constructor for the AboutWindow object
	 */
	public AboutWindowVideoJ() {
		Container top = this.getContentPane();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.add(version());
		top.add(licence());
		top.add(authors());
		top.add(institutions());
		top.add(contact());
		drawAbout();
	}


	/**
	 *  the version of the plugin
	 *
	 *@return    the label with the version
	 */
	public JLabel version() {
		JLabel label = new JLabel("VideoJ version 0.8");
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		return label;
	}


	/**
	 *  creates the licence label
	 *
	 *@return    the licence label
	 */
	public JLabel licence() {
		JLabel lic = new JLabel("distributed under the Licence Cecill");
		lic.setAlignmentX(Component.CENTER_ALIGNMENT);
		lic.setCursor(new Cursor(Cursor.HAND_CURSOR));
		lic.addMouseListener(
			new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					String url = "http://www.cecill.info/licences/Licence_CeCILL_V2-en.html";
					try {
						BrowserLauncher.openURL(url);
					}
					catch (IOException ioe) {
						IJ.log("cannot open the url " + url + "\n" + ioe);
					}
				}
			});
		return lic;
	}


	/**
	 *  creates the authors label
	 *
	 *@return    the authors label
	 */
	public JLabel authors() {
		JLabel curie = new JLabel("M. Husain, T. Boudier, P. Paul-Gilloteaux , S. Scheuring");
		curie.setAlignmentX(Component.CENTER_ALIGNMENT);
		return curie;
	}


	/**
	 *  creates the contact label
	 *
	 *@return    the contact label
	 */
	public JLabel contact() {
		JLabel cont = new JLabel("contact : simon.scheuring@curie.fr");
		cont.setAlignmentX(Component.CENTER_ALIGNMENT);
		cont.setCursor(new Cursor(Cursor.HAND_CURSOR));
		cont.addMouseListener(
			new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					try {
						BrowserLauncher.openURL("mailto:simon.scheuring@curie.fr");
					}
					catch (IOException ioe) {
						IJ.log("cannot open url mailto:simon.scheuring@curie.fr\n" + ioe);
					}
				}
			});
		return cont;
	}


	/**
	 *  creates the institutions label
	 *
	 *@return    the institutions label
	 */
	public JPanel institutions() {
		JPanel inst = new JPanel();
		inst.setLayout(new BoxLayout(inst, BoxLayout.X_AXIS));
		URL url = getClass().getResource("/icons/institut_curie.gif");
		Image image = Toolkit.getDefaultToolkit().getImage(url);
		ImageIcon icon = new ImageIcon(image);
		JLabel curie = new JLabel(icon, JLabel.CENTER);
		curie.setCursor(new Cursor(Cursor.HAND_CURSOR));
		curie.addMouseListener(
			new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					try {
						BrowserLauncher.openURL("http://www.curie.fr");
					}
					catch (IOException ioe) {
						IJ.log("cannot open url http://www.curie.fr\n" + ioe);
					}
				}
			});
		url = getClass().getResource("/icons/upmc.gif");
		image = Toolkit.getDefaultToolkit().getImage(url);
		icon = new ImageIcon(image);
		JLabel upmc = new JLabel(icon, JLabel.CENTER);
		upmc.setCursor(new Cursor(Cursor.HAND_CURSOR));
		//JLabel upmc = new JLabel(" Universite P. et M. Curie ");
		upmc.addMouseListener(
			new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					try {
						BrowserLauncher.openURL("http:/www.upmc.fr");
					}
					catch (IOException ioe) {
						IJ.log("cannot open url http://www.upmc.fr\n" + ioe);
					}
				}
			});
		url = getClass().getResource("/icons/cnrs.gif");
		image = Toolkit.getDefaultToolkit().getImage(url);
		icon = new ImageIcon(image);
		JLabel cnrs = new JLabel(icon, JLabel.CENTER);
		cnrs.setCursor(new Cursor(Cursor.HAND_CURSOR));
		//JLabel cnrs = new JLabel(" CNRS ");
		cnrs.addMouseListener(
			new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					try {
						BrowserLauncher.openURL("http://www.cnrs.fr");
					}
					catch (IOException ioe) {
						IJ.log("cannot open url http://www.cnrs.fr\n" + ioe);
					}
				}
			});
		url = getClass().getResource("/icons/inserm.gif");
		image = Toolkit.getDefaultToolkit().getImage(url);
		icon = new ImageIcon(image);
		JLabel inserm = new JLabel(icon, JLabel.CENTER);
		inserm.setCursor(new Cursor(Cursor.HAND_CURSOR));
		//JLabel cnrs = new JLabel(" CNRS ");
		inserm.addMouseListener(
			new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					try {
						BrowserLauncher.openURL("http://www.inserm.fr/fr/home.html");
					}
					catch (IOException ioe) {
						IJ.log("cannot open url http://www.inserm.fr\n" + ioe);
					}
				}
			});
		inst.add(curie);
		inst.add(upmc);
		//inst.add(cnrs);
		inst.add(inserm);
		return inst;
	}


	/**
	 *  draw the window
	 */
	private void drawAbout() {
		int sizeX = 600;
		Container top = this.getContentPane();
		int nbcomp = top.getComponentCount();
		for (int i = 0; i < nbcomp; i++) {
			Component tmp = top.getComponent(i);
			Dimension dim = tmp.getMinimumSize();
			tmp.setSize(sizeX, (int) dim.getHeight());
		}
		setSize(sizeX, 160);
		setResizable(false);
		setVisible(true);
	}
}

