package git.juanpablopinza.clients.infrastructure.adapter.in.web;

import git.juanpablopinza.clients.application.port.in.ClienteUseCase;
import git.juanpablopinza.clients.domain.exception.ClienteNoEncontradoException;
import git.juanpablopinza.clients.domain.model.Cliente;
import git.juanpablopinza.clients.domain.model.Genero;
import git.juanpablopinza.clients.domain.model.Identificacion;
import git.juanpablopinza.clients.infrastructure.adapter.in.web.mapper.ClienteWebMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteController.class)
@Import(ClienteWebMapperImpl.class)
class ClienteControllerTest {

	private static final String CLIENTE_JSON = """
			{
			  "nombre": "Jose Lema",
			  "genero": "MASCULINO",
			  "edad": 35,
			  "identificacion": "1710034065",
			  "direccion": "Otavalo sn y principal",
			  "telefono": "098254785",
			  "contrasena": "1234",
			  "estado": true
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ClienteUseCase clienteUseCase;

	private Cliente joseLema() {
		return Cliente.crear("Jose Lema", Genero.MASCULINO, 35, new Identificacion("1710034065"),
				"Otavalo sn y principal", "098254785", "hash-1234", true);
	}

	@Test
	@DisplayName("POST /api/clientes devuelve 201, Location y nunca expone la contraseña")
	void crearCliente() throws Exception {
		Cliente cliente = joseLema();
		when(clienteUseCase.crear(any())).thenReturn(cliente);

		mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(CLIENTE_JSON))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith("/api/clientes/" + cliente.getClienteId())))
				.andExpect(jsonPath("$.clienteId").value(cliente.getClienteId().toString()))
				.andExpect(jsonPath("$.nombre").value("Jose Lema"))
				.andExpect(jsonPath("$.estado").value(true))
				.andExpect(jsonPath("$.contrasena").doesNotExist());
	}

	@Test
	@DisplayName("POST /api/clientes con datos inválidos devuelve 400 con el detalle por campo")
	void crearClienteInvalido() throws Exception {
		String invalido = CLIENTE_JSON.replace("1710034065", "1710034066").replace("\"Jose Lema\"", "\"\"");

		mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(invalido))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[?(@.campo == 'identificacion')]").exists())
				.andExpect(jsonPath("$.errores[?(@.campo == 'nombre')]").exists());
		verifyNoInteractions(clienteUseCase);
	}

	@Test
	@DisplayName("GET /api/clientes/{id} inexistente devuelve 404 con código de error")
	void obtenerInexistente() throws Exception {
		UUID clienteId = UUID.randomUUID();
		when(clienteUseCase.obtener(clienteId)).thenThrow(new ClienteNoEncontradoException(clienteId));

		mockMvc.perform(get("/api/clientes/{clienteId}", clienteId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.codigo").value("CLIENTE_NO_ENCONTRADO"));
	}

	@Test
	@DisplayName("GET /api/clientes/{id} con un id que no es UUID devuelve 400")
	void obtenerConIdInvalido() throws Exception {
		mockMvc.perform(get("/api/clientes/{clienteId}", "no-es-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
	}

	@Test
	@DisplayName("DELETE /api/clientes/{id} devuelve 204")
	void eliminar() throws Exception {
		UUID clienteId = UUID.randomUUID();

		mockMvc.perform(delete("/api/clientes/{clienteId}", clienteId))
				.andExpect(status().isNoContent());
		verify(clienteUseCase).eliminar(eq(clienteId));
	}
}
