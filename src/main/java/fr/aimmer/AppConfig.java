package fr.aimmer;

import java.io.File;

public record AppConfig(
		char mode,
		File inputFile,
		File outputDir,
		int offset,
		int step
)
{
	public AppConfig
	{
		if (mode != 'C' && mode != 'D')
			throw new IllegalArgumentException("Le mode doit être 'C' ou 'D'.");
		if (offset < 0 || offset > 255)
			throw new IllegalArgumentException("L'offset doit être compris entre 0 et 255.");
		if (step < 0 || step > 127)
			throw new IllegalArgumentException("Le step doit être compris entre 0 et 127.");
	}
}
