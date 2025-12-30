import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

const _channel = MethodChannel('arcore_hello');

void main() {
  runApp(const ArFlutterApp());
}

class ArFlutterApp extends StatelessWidget {
  const ArFlutterApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter + ARCore',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF1B5E20)),
        useMaterial3: true,
      ),
      home: const _HomeScreen(),
    );
  }
}

class _HomeScreen extends StatefulWidget {
  const _HomeScreen();

  @override
  State<_HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<_HomeScreen> {
  String? _error;
  bool _launching = false;

  Future<void> _launchArSession() async {
    setState(() {
      _error = null;
      _launching = true;
    });

    try {
      await _channel.invokeMethod<void>('launchHelloAr');
    } on PlatformException catch (error) {
      setState(() {
        _error = error.message ?? 'Failed to open AR session.';
      });
    } catch (error) {
      setState(() {
        _error = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          _launching = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('AR Plane Detection'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Explore AR Plane Detection',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 12),
            Text(
              'This sample combines Flutter with the existing ARCore "Hello AR" Java '
              'experience. Review the tips below before launching:',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            const SizedBox(height: 16),
            const _TipItem('Use an ARCore-compatible Android device.'),
            const _TipItem('Grant camera permission when prompted.'),
            const _TipItem('Move your device slowly to reveal detected planes.'),
            const Spacer(),
            if (_error != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 16.0),
                child: Text(
                  _error!,
                  style: TextStyle(color: Theme.of(context).colorScheme.error),
                ),
              ),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                onPressed: _launching ? null : _launchArSession,
                icon: _launching
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.vrpano_outlined),
                label: Text(_launching ? 'Opening…' : 'Launch AR Experience'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _TipItem extends StatelessWidget {
  const _TipItem(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6.0),
      child: Row(
        children: [
          Icon(Icons.check_circle_outline,
              color: Theme.of(context).colorScheme.primary),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              text,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ),
        ],
      ),
    );
  }
}
