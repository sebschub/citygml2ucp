package pik.clminputdata.tools;

/**
 * A symmetric matrix A(i,j) = A(j,i) filled with booleans, the diagonal is a
 * fixed value.
 * 
 * @author Sebastian Schubert
 * 
 */
public class SymmetricMatrixBoolean {

	/**
	 * The value of the diagonal.
	 */
	private final boolean diag;
	/**
	 * The boolean matrix
	 */
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

	/**
	 * Get A(i,j)
	 * 
	 * @param i
	 *            The row
	 * @param j
	 *            The column
	 * @return A(i,j)
	 */
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

	/**
	 * Set A(i,j). Since the diagonal has a fixed value set at the
	 * initialization set(i,i,val) has no effect.
	 * 
	 * @param i
	 *            The row
	 * @param j
	 *            The column
	 * @param val
	 *            The value to set to A(i,j)
	 */
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
