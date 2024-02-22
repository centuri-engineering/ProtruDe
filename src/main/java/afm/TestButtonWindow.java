package afm;
import javax.swing.*;

public class TestButtonWindow extends JFrame
{
	public TestButtonWindow (int imagePixel, int scanSize, double zScale, double convert, double radiusInAng, double radiuspeaksInAng)
	{
		super("Test Button Window");
		setDefaultCloseOperation (JFrame.DO_NOTHING_ON_CLOSE);
		JTextArea textArea = new JTextArea(
			"imagePixel: "+imagePixel+"\n"+
			"scanSize:   "+scanSize+"\n"+
			"zScale:     "+zScale+"\n"+
			"convert:    "+convert+"\n"+
			"radius:     "+radiusInAng+"\n"+
			"radiuspeaks:"+radiuspeaksInAng);
		add(textArea);
		setSize(250,100);
	}
	
//	public static void main(String[] args) 
//				{
//					TestButtonWindow window = new TestButtonWindow();
//					window.setVisible(true);
//				}
}
	
