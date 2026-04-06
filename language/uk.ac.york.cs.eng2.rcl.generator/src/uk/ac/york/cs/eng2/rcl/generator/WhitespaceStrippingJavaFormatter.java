package uk.ac.york.cs.eng2.rcl.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.epsilon.egl.formatter.language.LanguageFormatter;

public class WhitespaceStrippingJavaFormatter extends LanguageFormatter {

	// Increase indentation after every open bracket that terminates a line
	// (allowing for any whitespace between the bracket and the line terminator),
	// or a ( that terminates a line
	private static final String increasePattern = "[{(]\\s*$";
	
	// Decrease indentation after every close bracket that begins a line or )
	private static final String decreasePattern = "^([}]|[)];)";
	
	public WhitespaceStrippingJavaFormatter() {
		super(Pattern.compile(increasePattern, Pattern.MULTILINE),
		      Pattern.compile(decreasePattern, Pattern.MULTILINE));
	}
	
	@Override
	public String format(String text) {
		String indented = super.format(text);

		// Strip all trailing whitespace
		String[] indentedLines = Pattern.compile("^", Pattern.MULTILINE).split(indented);
		for (int i = 0; i < indentedLines.length; i++) {
			indentedLines[i] = indentedLines[i].stripTrailing();
		}

		// Remove unnecessary empty lines
		int i = 0;
		List<String> cleanedLines = new ArrayList<>(indentedLines.length); 
		while (i < indentedLines.length) {
			final String line = indentedLines[i];
			if (!line.isEmpty()) {
				cleanedLines.add(line);
			} else if (i + 1 < indentedLines.length) {
				final String nextLine = indentedLines[i+1];
				if (!nextLine.isBlank() && !nextLine.strip().startsWith("}")) {
					cleanedLines.add(line);
				}
			}

			i++;
		}

		return String.join(System.lineSeparator(), cleanedLines);
	}

}