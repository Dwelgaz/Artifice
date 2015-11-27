package com.artifice.refactoring.log;

public class LoggingUnit implements Comparable<LoggingUnit>{

	private int startPosition = 0;
	public int getStartPosition() {
		return this.startPosition;
	}
	
	private String transformation = "";
	public String getTransformation() {
		return this.transformation;
	}
	
	public LoggingUnit(int start, String transformation) {
		this.startPosition = start;
		this.transformation = transformation;
	}
	
	@Override
	public String toString() {
		return "[" + this.startPosition + "] --- " + this.transformation + System.getProperty("line.separator") + System.getProperty("line.separator");
	}

	@Override
	public int compareTo(LoggingUnit unit) {
		if (this.startPosition < unit.startPosition)
			return -1;
		else if (this.startPosition == unit.startPosition)
			return 0;
		else
			return 1;
	}
}
