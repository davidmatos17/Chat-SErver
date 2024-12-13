import os
import subprocess

# Configurações do cliente
server_address = "localhost"  # Endereço do servidor
server_port = "8000"        # Porta do servidor

# Função para iniciar 3 instâncias do ChatClient
def start_chat_clients():
    for i in range(2):
        print(f"Iniciando ChatClient {i + 1}...")
        try:
            # Executa o ChatClient diretamente em processos separados
            subprocess.Popen(["java", "ChatClient", server_address, server_port])
        except FileNotFoundError as e:
            print(f"Erro: {e}. Certifique-se de que o Java está instalado e configurado corretamente.")
            exit(1)
        except Exception as e:
            print(f"Erro inesperado: {e}")
            exit(1)

if __name__ == "__main__":
    start_chat_clients()