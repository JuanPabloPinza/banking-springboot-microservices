package git.juanpablopinza.clients.application.port.out;

import git.juanpablopinza.clients.domain.event.ClienteEvento;

public interface ClienteEventoPublisherPort {

	void publicar(ClienteEvento evento);
}
