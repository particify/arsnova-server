package de.thm.arsnova.service;

import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.transport.ImportExportContainer;

public interface ImportService {
	Room importFromV2(ImportExportContainer container);
}
