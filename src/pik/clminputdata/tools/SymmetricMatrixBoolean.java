package pik.clminputdata.tools;

public class SymmetricMatrixBoolean {

	private final boolean diag;
	private boolean[][] field;

	public SymmetricMatrixBoolean(int a, boolean diag) {
		this.diag = diag;
		// first dimension complete
		field = new boolean[a][];
		for (int i = 0; i < a; i++) {
			// only lower part without diagonal
			field[i] = new boolean[i];
		}
	}

	public boolean get(int i, int j) {
		if (i > j) {
			return field[i][j];
		} else if (i < j) {
			// use symmetry of matrix
			return field[j][i];
		} else {
			// return diag values
			return diag;
		}
	}

	public void set(int i, int j, boolean val) {
		if (i > j) {
			// System.out.println(i+" " + j + " " + k + " " + l);
			field[i][j] = val;
		} else if (i < j) {
			// use symmetry of matrix
			field[j][i] = val;
		}
		// else case not defined
	}

}
