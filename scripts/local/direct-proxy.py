"""Minimal direct HTTP/HTTPS proxy for the local WeChat DevTools upload path.
DevTools is configured to use 127.0.0.1:10301, but nothing listens there.
This proxy connects targets directly; it has no upstream and no external side effects
beyond the requests the IDE itself makes. Local development use only.
"""
import socket, threading, select

HOST, PORT = '127.0.0.1', 10301

def relay(client, remote):
    sockets = [client, remote]
    try:
        while True:
            readable, _, broken = select.select(sockets, [], sockets, 60)
            if broken or not readable:
                return
            for source in readable:
                data = source.recv(65536)
                if not data:
                    return
                (remote if source is client else client).sendall(data)
    except OSError:
        return

def handle(client):
    remote = None
    try:
        client.settimeout(15)
        request = b''
        while b'\r\n\r\n' not in request and len(request) < 65536:
            chunk = client.recv(4096)
            if not chunk:
                client.close()
                return
            request += chunk
        head, _, rest = request.partition(b'\r\n\r\n')
        line = head.split(b'\r\n', 1)[0].decode('latin1')
        method, target, _ = line.split(' ', 2)
        if method.upper() == 'CONNECT':
            host, port = target.rsplit(':', 1)
            remote = socket.create_connection((host, int(port)), timeout=15)
            client.sendall(b'HTTP/1.1 200 Connection Established\r\n\r\n')
            if rest:
                remote.sendall(rest)
        else:
            # Plain HTTP absolute-form request: forward as-is.
            from urllib.parse import urlsplit
            parts = urlsplit(target)
            host = parts.hostname or ''
            port = parts.port or 80
            remote = socket.create_connection((host, port), timeout=15)
            remote.sendall(request)
        relay(client, remote)
    except OSError:
        try:
            client.sendall(b'HTTP/1.1 502 Bad Gateway\r\n\r\n')
        except OSError:
            pass
    finally:
        for sock in (client, remote):
            try:
                if sock:
                    sock.close()
            except OSError:
                pass

def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((HOST, PORT))
    server.listen(64)
    print(f'Direct proxy listening on {HOST}:{PORT}', flush=True)
    while True:
        client, _ = server.accept()
        threading.Thread(target=handle, args=(client,), daemon=True).start()

if __name__ == '__main__':
    main()
