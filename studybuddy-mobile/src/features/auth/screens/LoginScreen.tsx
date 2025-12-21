import React, { useMemo, useState } from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { Screen } from '../../../components/ui/Screen';
import { TextField } from '../../../components/ui/TextField';
import { Button } from '../../../components/ui/Button';
import { typography } from '../../../theme/typography';
import { spacing } from '../../../theme/spacing';
import { useAuth } from '../../../auth/AuthContext';
import { useToast } from '../../../components/ui/ToastProvider';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { AuthStackParamList } from '../../../navigation/AuthStack';
import { useAppTheme, Palette } from '../../../theme/ThemeProvider';

const schema = z.object({
  username: z.string().min(3, 'Username is required'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
});

type FormValues = z.infer<typeof schema>;

const LoginScreen: React.FC<NativeStackScreenProps<AuthStackParamList, 'Login'>> = ({ navigation }) => {
  const { login } = useAuth();
  const { showToast } = useToast();
  const { colors } = useAppTheme();
  const styles = useMemo(() => createStyles(colors), [colors]);
  const [loading, setLoading] = useState(false);

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { username: '', password: '' } });

  const onSubmit = async (values: FormValues) => {
    setLoading(true);
    try {
      await login({ username: values.username, password: values.password });
      showToast('Welcome back!', 'success');
    } catch (error: any) {
      const message = error?.response?.data?.message ?? 'Login failed. Please try again.';
      showToast(message, 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Screen>
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>StudyBuddy</Text>
          <Text style={styles.subtitle}>Sign in to continue your learning journey.</Text>
        </View>

        <View style={styles.form}>
          <TextField
            label="Username"
            name="username"
            control={control}
            autoCapitalize="none"
            autoCorrect={false}
            error={errors.username?.message}
            returnKeyType="next"
          />
          <TextField
            label="Password"
            name="password"
            control={control}
            secure
            error={errors.password?.message}
            returnKeyType="done"
          />
          <Button label="Sign In" onPress={handleSubmit(onSubmit)} loading={loading} disabled={loading} />
        </View>

        <TouchableOpacity onPress={() => navigation.navigate('Register')} accessibilityRole="button">
          <Text style={styles.link}>Need an account? Create one</Text>
        </TouchableOpacity>
      </View>
    </Screen>
  );
};

const createStyles = (colors: Palette) =>
  StyleSheet.create({
    container: {
      flex: 1,
      justifyContent: 'space-between',
    },
    header: {
      gap: spacing.sm,
    },
    title: {
      fontSize: 32,
      fontWeight: '700',
      color: colors.textPrimary,
    },
    subtitle: {
      fontSize: typography.body,
      color: colors.textSecondary,
    },
    form: {
      gap: spacing.md,
      paddingTop: spacing.lg,
    },
    link: {
      textAlign: 'center',
      color: colors.primary,
      fontSize: 16,
      marginTop: spacing.lg,
      fontWeight: '600',
    },
  });

export default LoginScreen;
