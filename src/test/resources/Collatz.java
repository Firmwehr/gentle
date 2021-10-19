class Collatz {
	public static int collatz(int n) {
		int i = 0;
		while (n != 1) {
			if (n % 2 == 0) {
				n /= 2;
			} else {
				n = 3 * n + 1;
			}
			i++;
		}
		return i;
	}
}