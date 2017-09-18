package nl.pim16aap2.bigDoors.moveBlocks.Cylindrical;

public class CylindricalMover {
	

	
	// Figure out the divider for the width.
	public double getDivider(int len) 
	{
		double divider = 1;
		switch (len-1)
		{
		case 1:
			divider = 38;
			break;
		case 2:
			divider = 30;
			break;
		case 3:
			divider = 23;
			break;
		case 4:
			divider = 18;
			break;
		case 5:
			divider = 15;
			break;
		case 6:
			divider = 13;
			break;
		case 7:
			divider = 5.5;
			break;
		case 8:
			divider = 10;
			break;
		case 9:
			divider = 8.8;
			break;
		case 10:
			divider = 8.2;
			break;
		case 11:
			divider = 7.3;
			break;
		case 12:
			divider = 6.8;
			break;
		case 13:
			divider = 6.4;
			break;
		case 14:
			divider = 6.0;
			break;
		case 15:
			divider = 5.6;
			break;
		case 16:
			divider = 5.2;
			break;
		case 17:
			divider = 4.9;
			break;
		case 18:
			divider = 4.7;
			break;
		case 19:
			divider = 38;
			break;
		case 20:
			divider = 38;
			break;
		case 21:
			divider = 38;
			break;
		case 22:
			divider = 38;
			break;
		case 23:
			divider = 38;
			break;
		case 24:
			divider = 38;
			break;
		case 25:
			divider = 38;
			break;
		case 26:
			divider = 38;
			break;
		}
		return divider;
	}
	
}
