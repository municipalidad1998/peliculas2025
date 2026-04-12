import 'package:flutter/material.dart';
import 'screens/home_screen.dart';

void main() {
  runApp(const DoramaNetflixApp());
}

class DoramaNetflixApp extends StatelessWidget {
  const DoramaNetflixApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Doramflix',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        primarySwatch: Colors.red,
        scaffoldBackgroundColor: Colors.black,
        appBarTheme: const AppBarTheme(
          backgroundColor: Colors.black,
        ),
      ),
      home: const HomeScreen(),
    );
  }
}
