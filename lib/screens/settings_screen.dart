import 'package:flutter/material.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF0F1715),
      child: SafeArea(
        child: ListView(
          padding: const EdgeInsets.symmetric(vertical: 8),
          children: [
            _buildSettingsItem(
              icon: Icons.shield_outlined,
              title: "Contenido y seguridad",
              subtitle: "Acceso a TMDb, control parental y restricciones de contenido",
            ),
            _buildSettingsItem(
              icon: Icons.play_circle_outline,
              title: "Ajustes del reproductor",
              subtitle: "Reproducción automática, búfer, reproducción de tráilers y...",
            ),
            _buildSettingsItem(
              icon: Icons.palette_outlined,
              title: "Apariencia",
              subtitle: "Tema, modo inmersivo y ajustes del diseño de pantalla",
            ),
            _buildSettingsItem(
              icon: Icons.wifi,
              title: "Conexión y servicios",
              subtitle: "DNS, bypass para TV y claves de servicios de subtítulos",
            ),
            _buildSettingsItem(
              icon: Icons.extension_outlined,
              title: "Ajustes para el proveedor act...",
              subtitle: "Dominio, portal y opciones de recuperación específicas de TMDb (es)",
            ),
            _buildSettingsItem(
              icon: Icons.restore,
              title: "Copia de seguridad y restaura...",
              subtitle: "Exporta o restaura favoritos, historial y datos guardados de la app",
              onTap: () => _showBackupMenu(context),
            ),
            const Padding(
              padding: EdgeInsets.only(left: 16, top: 16, bottom: 8),
              child: Text("Varios", style: TextStyle(color: Color(0xFF40E0D0), fontWeight: FontWeight.bold)),
            ),
            ListTile(
              title: const Text("Búsqueda de actualizaciones automática", style: TextStyle(color: Colors.white)),
              subtitle: const Text("Buscar nuevas versiones de la aplicación automáticamente", style: TextStyle(color: Colors.white54, fontSize: 13)),
              trailing: Switch(
                value: true,
                onChanged: (val) {},
                activeColor: const Color(0xFF40E0D0),
                activeTrackColor: const Color(0xFF1B3B36),
              ),
            ),
            ListTile(
              leading: const Icon(Icons.telegram, color: Colors.white70),
              title: const Text("Grupo de Telegram", style: TextStyle(color: Colors.white)),
              onTap: () {},
            ),
            const ListTile(
              leading: Icon(Icons.info_outline, color: Colors.white70),
              title: Text("Versión para móviles", style: TextStyle(color: Colors.white)),
              subtitle: Text("1.7.112", style: TextStyle(color: Colors.white54)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSettingsItem({required IconData icon, required String title, required String subtitle, VoidCallback? onTap}) {
    return ListTile(
      leading: Container(
        height: 60,
        alignment: Alignment.center,
        width: 30,
        child: Icon(icon, color: Colors.white70),
      ),
      title: Text(title, style: const TextStyle(color: Colors.white, fontSize: 16)),
      subtitle: Text(subtitle, style: const TextStyle(color: Colors.white54, fontSize: 13)),
      trailing: const Icon(Icons.chevron_right, color: Colors.white54, size: 20),
      minVerticalPadding: 12,
      onTap: onTap ?? () {},
    );
  }

  void _showBackupMenu(BuildContext context) {
    Navigator.of(context).push(MaterialPageRoute(builder: (c) => const BackupScreen()));
  }
}

class BackupScreen extends StatelessWidget {
  const BackupScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0F1715),
      appBar: AppBar(
        title: const Text("Copia de seguridad y restauración", style: TextStyle(color: Color(0xFF40E0D0), fontSize: 18)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.white),
      ),
      body: ListView(
        children: [
          _buildBackupItem("Exportar datos de usuario", "Guarda tus datos (favoritos, historial de visualización) en un archivo."),
          _buildBackupItem("Importar datos de usuario", "Restaura tus datos desde un archivo de copia de seguridad."),
          _buildBackupItem("Actualizar caché desde la base de datos", "Recargar el historial de visualización y los datos de la app desde la base de datos"),
          _buildBackupItem("Exportar copia de seguridad de la base de datos", "Guarda una instantánea ZIP de las bases de datos de la app."),
          _buildBackupItem("Importar copia de seguridad de la base de datos", "Restaura las bases de datos de la app desde una instantánea ZIP."),
        ],
      ),
    );
  }

  Widget _buildBackupItem(String title, String sub) {
    return ListTile(
      title: Text(title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
      subtitle: Padding(
        padding: const EdgeInsets.only(top: 4.0),
        child: Text(sub, style: const TextStyle(color: Colors.white70)),
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
      onTap: () {},
    );
  }
}
