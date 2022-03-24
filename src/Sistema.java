// PUCRS - Escola Politécnica - Sistemas Operacionais
// Prof. Fernando Dotti
// Código fornecido como parte da solução do projeto de Sistemas Operacionais
//
// Fase 1 - máquina virtual (vide enunciado correspondente)
//

import java.sql.SQLOutput;
import java.util.*;
public class Sistema {

    // -------------------------------------------------------------------------------------------------------
    // --------------------- H A R D W A R E - definicoes de HW ----------------------------------------------

    // -------------------------------------------------------------------------------------------------------
    // --------------------- M E M O R I A -  definicoes de opcode e palavra de memoria ----------------------

    public class Word { 	// cada posicao da memoria tem uma instrucao (ou um dado)
        public Opcode opc; 	//
        public int r1; 		// indice do primeiro registrador da operacao (Rs ou Rd cfe opcode na tabela)
        public int r2; 		// indice do segundo registrador da operacao (Rc ou Rs cfe operacao)
        public int p; 		// parametro para instrucao (k ou A cfe operacao), ou o dado, se opcode = DADO

        public Word(Opcode _opc, int _r1, int _r2, int _p) {
            opc = _opc;   r1 = _r1;    r2 = _r2;	p = _p;
        }
    }
    // -------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------
    // --------------------- C P U  -  definicoes da CPU -----------------------------------------------------

    public enum Opcode {
        DATA, ___,		    // se memoria nesta posicao tem um dado, usa DATA, se não usada é NULO ___
        JMP, JMPI, JMPIG, JMPIL, JMPIE, JMPIM, JMPIGM, JMPILM, JMPIEM, STOP,   // desvios e parada
        ADDI, SUBI, ADD, SUB, MULT,             // matemáticos
        LDI, LDD, STD, LDX, STX, SWAP,          // movimentação
        TRAP;                                   //
    }

    public enum Interrupts {
        INT_NONE,
        INT_INVALID_INSTRUCTION,    // Nunca será usada, pois o Java não deixará compilar
        INT_INVALID_ADDRESS,        // Nossa memória tem 1024 posições
        INT_OVERFLOW,               // Nossa memória só trabalha com inteiros, ou seja de -2,147,483,648 até 2,147,483,647
        INT_SYSTEM_CALL;            // Ativa chamada de I/O pelo comando TRAP
    }

    public class CPU {
        // característica do processador: contexto da CPU ...
        private int pc; 			// ... composto de program counter,
        private Word ir; 			// instruction register,
        private int[] reg;       	// registradores da CPU
        public int maxInt;          // criado para podermos simular overflow

        // cria variável interrupção
        public Interrupts interrupts;

        private Word[] m;   // CPU acessa MEMORIA, guarda referencia 'm' a ela. memoria nao muda. ee sempre a mesma.

        public CPU(Word[] _m) {     // ref a MEMORIA e interrupt handler passada na criacao da CPU
            m = _m; 				// usa o atributo 'm' para acessar a memoria.
            reg = new int[10]; 		// aloca o espaço dos registradores
            maxInt = 1000;
        }

        public void setContext(int _pc) {  // no futuro esta funcao vai ter que ser
            pc = _pc;                                   // limite e pc (deve ser zero nesta versão)
            this.interrupts = Interrupts.INT_NONE;      // imnicializa interrupção com NONE
        }

        private void dump(Word w) {
            System.out.print("[ ");
            System.out.print(w.opc); System.out.print(", ");
            System.out.print(w.r1);  System.out.print(", ");
            System.out.print(w.r2);  System.out.print(", ");
            System.out.print(w.p);  System.out.println("  ] ");
        }

        private void showState(){
            System.out.println("       "+ pc);
            System.out.print("           ");
            for (int i=0; i<8; i++) { System.out.print("r"+i);   System.out.print(": "+reg[i]+"     "); };
            System.out.println("");
            System.out.print("           ");  dump(ir);
        }

        private boolean isAddressValid(int address) {
            if (address < 0 || address >= 1024) {
                interrupts = Interrupts.INT_INVALID_ADDRESS;
                return false;
            }
            return true;
        }

        public void run() { 		// execucao da CPU supoe que o contexto da CPU, vide acima, esta devidamente setado
            //System.out.println("Início da execução pela CPU");

            boolean run = true;
            while (run) { 			// ciclo de instrucoes. acaba cfe instrucao, veja cada caso.
                // FETCH
                ir = m[pc]; 	// busca posicao da memoria apontada por pc, guarda em ir
                //if debug
                showState();
                // EXECUTA INSTRUCAO NO ir
                switch (ir.opc) { // para cada opcode, sua execução

                    case LDI: // Rd ← k
                        reg[ir.r1] = ir.p;
                        pc++;
                        break;

                    case LDD: // Rd ← [A]
                        if (isAddressValid(ir.p))
                            {
                                reg[ir.r1] = m[ir.p].p;
                                pc++;
                                break;
                            }
                        else
                            break;

                    case STD: // [A] ← Rs
                        m[ir.p].opc = Opcode.DATA;
                        m[ir.p].p = reg[ir.r1];
                        pc++;
                        break;

                    case ADD: // Rd ← Rd + Rs
                        reg[ir.r1] = reg[ir.r1] + reg[ir.r2];
                        pc++;
                        break;

                    case MULT: // Rd ← Rd * Rs
                        if (reg[ir.r1] * reg[ir.r2] > maxInt)
                            {
                            interrupts = Interrupts.INT_OVERFLOW;
                            pc++;
                            break;
                            }
                        else
                            {
                            reg[ir.r1] = reg[ir.r1] * reg[ir.r2];
                            pc++;
                            break;
                            }

                    case ADDI: // Rd ← Rd + k
                        reg[ir.r1] = reg[ir.r1] + ir.p;
                        pc++;
                        break;

                    case STX: // [Rd] ←Rs
                        m[reg[ir.r1]].opc = Opcode.DATA;
                        m[reg[ir.r1]].p = reg[ir.r2];
                        pc++;
                        break;

                    case LDX: // Rd ← [Rs]
                        reg[ir.r1] = m[reg[ir.r2]].p;
                        pc++;
                        break;

                    case SUB: // Rd ← Rd - Rs
                        reg[ir.r1] = reg[ir.r1] - reg[ir.r2];
                        pc++;
                        break;

                    case SUBI: // Rd ← Rd - k
                        reg[ir.r1] = reg[ir.r1] - ir.p;
                        pc++;
                        break;

                    case JMP: //  PC ← k
                        pc = ir.p;
                        break;

                    case JMPI: //  PC ← Rs
                        pc = ir.r1;
                        break;

                    case JMPIG: // If Rc > 0 Then PC ← Rs Else PC ← PC +1
                        if (reg[ir.r2] > 0) {
                            pc = reg[ir.r1];
                        } else {
                            pc++;
                        }
                        break;

                    case JMPIGM: // If Rc > 0 Then PC ← [A] Else PC ← PC +1
                        //if (reg[ir.r1] > 0) {
                        //    pc = ir.p;
                        if (reg[ir.r2] > 0) {
                            pc = m[ir.p].p;
                        } else {
                            pc++;
                        }
                        break;

                    case JMPILM: // If Rc < 0 Then PC ← [A] Else PC ← PC +1
                        //if (reg[ir.r1] < 0) {
                        //    pc = ir.p;
                        if (reg[ir.r2] < 0) {
                            pc = m[ir.p].p;
                        } else {
                            pc++;
                        }
                        break;

                    case JMPIEM: // If Rc = 0 Then PC ← [A] Else PC ← PC +1
                        //if (reg[ir.r1] == 0) {
                        //    pc = ir.p;
                        if (reg[ir.r2] == 0) {
                            pc = m[ir.p].p;
                        } else {
                            pc++;
                        }
                        break;

                    case JMPIE: // If Rc = 0 Then PC ← Rs Else PC ← PC +1
                        if (reg[ir.r2] == 0) {
                            pc = reg[ir.r1];
                        } else {
                            pc++;
                        }
                        break;

                    case JMPIL: //  PC ← Rs
                        if (reg[ir.r2] < 0) {
                            pc = reg[ir.r1];
                        } else {
                            pc++;
                        }
                        break;

                    case JMPIM: //  PC ← [A]
                        pc = m[ir.p].p;
                        break;

                    case SWAP: // t <- r1; r1 <- r2; r2 <- t
                        int temp;
                        temp = reg[ir.r1];
                        reg[ir.r1] = reg[ir.r2];
                        reg [ir.r2] = temp;
                        pc ++;
                        break;

                    case STOP: // por enquanto, para execucao
                        break;

                    case TRAP:
                        interrupts = Interrupts.INT_SYSTEM_CALL;
                        pc ++;
                        break;

                    case DATA:
                        pc++;
                        break;

                    default:
                        // opcode desconhecido
                        interrupts = Interrupts.INT_INVALID_INSTRUCTION;
                }

                // VERIFICA INTERRUPÇÃO !!! - TERCEIRA FASE DO CICLO DE INSTRUÇÕES
                if (ir.opc==Opcode.STOP)
                    {
                    break; // break sai do loop da cpu
                    }

                if (interrupts != Interrupts.INT_NONE)
                    switch (interrupts){
                        case INT_INVALID_ADDRESS:
                            System.out.println("Endereço inválido!");
                            run = false;
                            break;

                        // Case implementado, mas nunca será usado, pois em Java não será nem permitido compilar o programa com uma instrução inválida
                        case INT_INVALID_INSTRUCTION:
                            System.out.println("Comando desconhecido!");
                            run = false;
                            break;

                        case INT_OVERFLOW:
                            System.out.println("Deu overflow!");
                            run = false;
                            break;

                        case INT_SYSTEM_CALL:
                            // Entrada (in) (reg[8]=1): o programa lê um inteiro do teclado.
                            // O parâmetro para IN, em reg[9], é o endereço de memória a armazenar a leitura
                            // Saída (out) (reg[8]=2): o programa escreve um inteiro na tela.
                            // O parâmetro para OUT, em reg[9], é o endereço de memória cujo valor deve-se escrever na tela

                            Scanner in = new Scanner(System.in);

                            if (reg[8]==1){
                                int address_destiny = reg[9];
                                int value_to_be_written = in.nextInt();
                                m[address_destiny].p = value_to_be_written;
                            }

                            if (reg[8]==2){
                                int source_adress = reg[9];
                                System.out.println("Output: " + m[source_adress].p);
                            }

                            break;
                    }

            }
        }
    }
    // ------------------ C P U - fim ------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------


    // ------------------- V M  - constituida de CPU e MEMORIA -----------------------------------------------
    // -------------------------- atributos e construcao da VM -----------------------------------------------
    public class VM {
        public int tamMem;
        public Word[] m;
        public CPU cpu;

        public VM(){
            // memória
            tamMem = 1024;
            m = new Word[tamMem]; // m ee a memoria
            for (int i=0; i<tamMem; i++) { m[i] = new Word(Opcode.___,-1,-1,-1); };

            // cpu
            cpu = new CPU(m);   // cpu acessa memória
        }
    }
    // ------------------- V M  - fim ------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

    // --------------------H A R D W A R E - fim -------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------
    // ------------------- S O F T W A R E - inicio ----------------------------------------------------------

    // -------------------------------------------  funcoes de um monitor
    public class Monitor {
        public void dump(Word w) {
            System.out.print("[ ");
            System.out.print(w.opc); System.out.print(", ");
            System.out.print(w.r1);  System.out.print(", ");
            System.out.print(w.r2);  System.out.print(", ");
            System.out.print(w.p);  System.out.println("  ] ");
        }

        public void dump(Word[] m, int ini, int fim) {
            for (int i = ini; i < fim; i++) {
                System.out.print(i); System.out.print(":  ");  dump(m[i]);
            }
        }

        public void carga(Word[] p, Word[] m) {    // significa ler "p" de memoria secundaria e colocar na principal "m"
            for (int i = 0; i < p.length; i++) {
                m[i].opc = p[i].opc;
                m[i].r1 = p[i].r1;
                m[i].r2 = p[i].r2;
                m[i].p = p[i].p;
            }
        }

        public void executa() {
            vm.cpu.setContext(0);          // monitor seta contexto - pc aponta para inicio do programa
            vm.cpu.run();                  //                         e cpu executa
            // note aqui que o monitor espera que o programa carregado acabe normalmente
            // nao ha protecoes...  o que poderia acontecer ?
        }
    }
    // -------------------------------------------




    // -------------------------------------------------------------------------------------------------------
    // -------------------  S I S T E M A --------------------------------------------------------------------

    public VM vm;
    public Monitor monitor;
    public static Programas progs;

    public Sistema(){   // a VM com tratamento de interrupções
        vm = new VM();
        monitor = new Monitor();
        progs = new Programas();
    }

    public void roda(Word[] programa){
        monitor.carga(programa, vm.m);
        System.out.println("---------------------------------- programa carregado ");

        monitor.dump(vm.m, 0, programa.length);

        monitor.executa();
        System.out.println("---------------------------------- após execucao ");

        monitor.dump(vm.m, 0, programa.length);
    }

    // -------------------  S I S T E M A - fim --------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------
    // ------------------- instancia e testa sistema
    public static void main(String args[]) {
        Sistema s = new Sistema();
        // Desenvolvidos pelo professor
        //s.roda(progs.fibonacci10);           // "progs" significa acesso/referencia ao programa em memoria secundaria
        // s.roda(progs.progMinimo);
        //s.roda(progs.fatorial);

        // Desenvolvidos pelo grupo
        s.roda(progs.bubbleSort);

        // Testes de Funcionalidades
        //s.roda(progs.invalidAddressTest);
        //s.roda(progs.overflowTest);
        //s.roda(progs.trapTestOutput);
        //s.roda(progs.trapTestInput);

    }
    // -------------------------------------------------------------------------------------------------------
    // --------------- TUDO ABAIXO DE MAIN É AUXILIAR PARA FUNCIONAMENTO DO SISTEMA - nao faz parte

    //  -------------------------------------------- programas aa disposicao para copiar na memoria (vide carga)
    public class Programas {
        public Word[] progMinimo = new Word[] {
                //       OPCODE      R1  R2  P         :: VEJA AS COLUNAS VERMELHAS DA TABELA DE DEFINICAO DE OPERACOES
                //                                     :: -1 SIGNIFICA QUE O PARAMETRO NAO EXISTE PARA A OPERACAO DEFINIDA
                new Word(Opcode.LDI, 0, -1, 999),
                new Word(Opcode.STD, 0, -1, 10),
                new Word(Opcode.STD, 0, -1, 11),
                new Word(Opcode.STD, 0, -1, 12),
                new Word(Opcode.STD, 0, -1, 13),
                new Word(Opcode.STD, 0, -1, 14),
                new Word(Opcode.STOP, -1, -1, -1) };

        public Word[] fibonacci10 = new Word[] { // mesmo que prog exemplo, so que usa r0 no lugar de r8
                new Word(Opcode.LDI, 1, -1, 0),  //0 coloca 0 no reg 1
                new Word(Opcode.STD, 1, -1, 20),    // 20 posicao de memoria onde inicia a serie de fibonacci gerada //1, ou seja coloca valor de reg 1 (0) na posicao 20
                new Word(Opcode.LDI, 2, -1, 1), //2 coloca 1 no reg 2
                new Word(Opcode.STD, 2, -1, 21), //3 na posição 21 coloca o que está em no reg 2, ou seja coloca 1 na posicao 21
                new Word(Opcode.LDI, 0, -1, 22), //4 coloca 22 no reg 0
                new Word(Opcode.LDI, 6, -1, 6), //5 coloca 6 no reg 6 - linha do inicio do loop
                new Word(Opcode.LDD, 7, -1, 17), //6 coloca 17 no reg 7. É o contador. será a posição one começam os dados, ou seja 20 + a quantidade de números fibonacci que queremos
                // new Word(Opcode.LDI, 7, -1, 31), //6 coloca 31 no reg 7. É o contador. será a posição one começam os dados, ou seja 20 + a quantidade de números fibonacci que queremos
                new Word(Opcode.LDI, 3, -1, 0), //7 coloca 0 no reg 3
                new Word(Opcode.ADD, 3, 1, -1), //8
                new Word(Opcode.LDI, 1, -1, 0), //9
                new Word(Opcode.ADD, 1, 2, -1), //10 add reg 1 + reg 2
                new Word(Opcode.ADD, 2, 3, -1), //11 add reg 2 + reg 3
                new Word(Opcode.STX, 0, 2, -1), //12 coloca o que está em reg 2 (1) na posição  memória do reg 0 (22), ou seja coloca 1 na pos 22
                new Word(Opcode.ADDI, 0, -1, 1), //13 add 1 no reg 0, ou seja reg fica com 23. Isso serve para mudar a posição da memória onde virá o próximo numero fbonacci
                new Word(Opcode.SUB, 7, 0, -1), //14 reg 7 = reg 7 - o que esta no reg 0, ou seja 30 menos 23 e coloca em r7. Isso é o contador regressivo que fica em r7. se for 0, pára
                new Word(Opcode.JMPIG, 6, 7, -1), //15 se r7 maior que 0 então pc recebe 6, else pc = pc + 1
                new Word(Opcode.STOP, -1, -1, -1),   // POS 16
                new Word(Opcode.DATA, -1, -1, 31), //17 numeros de fibonacci a serem calculados menos 20
                new Word(Opcode.DATA, -1, -1, -1), //18
                new Word(Opcode.DATA, -1, -1, -1), //19
                new Word(Opcode.DATA, -1, -1, -1),   // POS 20
                new Word(Opcode.DATA, -1, -1, -1), //21
                new Word(Opcode.DATA, -1, -1, -1), //22
                new Word(Opcode.DATA, -1, -1, -1), //23
                new Word(Opcode.DATA, -1, -1, -1), //24
                new Word(Opcode.DATA, -1, -1, -1), //25
                new Word(Opcode.DATA, -1, -1, -1), //26
                new Word(Opcode.DATA, -1, -1, -1), //27
                new Word(Opcode.DATA, -1, -1, -1), //28
                new Word(Opcode.DATA, -1, -1, -1),  // ate aqui - serie de fibonacci ficara armazenada //29
                new Word(Opcode.DATA, -1, -1, -1)
        };

        public Word[] fatorial = new Word[] { 	 // este fatorial so aceita valores positivos.   nao pode ser zero
                // linha   coment
                new Word(Opcode.LDI, 0, -1, 6),      // 0   	r0 é valor a calcular fatorial
                new Word(Opcode.LDI, 1, -1, 1),      // 1   	r1 é 1 para multiplicar (por r0)
                new Word(Opcode.LDI, 6, -1, 1),      // 2   	r6 é 1 para ser o decremento
                new Word(Opcode.LDI, 7, -1, 8),      // 3   	r7 tem posicao de stop do programa = 8
                new Word(Opcode.JMPIE, 7, 0, 0),     // 4   	se r0=0 pula para r7(=8)
                new Word(Opcode.MULT, 1, 0, -1),     // 5   	r1 = r1 * r0
                new Word(Opcode.SUB, 0, 6, -1),      // 6   	decrementa r0 1
                new Word(Opcode.JMP, -1, -1, 4),     // 7   	vai p posicao 4
                new Word(Opcode.STD, 1, -1, 10),     // 8   	coloca valor de r1 na posição 10
                new Word(Opcode.STOP, -1, -1, -1),    // 9   	stop
                new Word(Opcode.DATA, -1, -1, -1) };  // 10   ao final o valor do fatorial estará na posição 10 da memória

        public Word[] invalidAddressTest = new Word []{
                new Word(Opcode.LDD, 0, -1, 1025),
                new Word(Opcode.STOP, -1, -1, -1)
        };

        public Word[] overflowTest = new Word []{
                new Word(Opcode.LDI, 0, -1, 800),
                new Word(Opcode.LDI, 1, -1, 800),
                new Word(Opcode.MULT, 0, 1, -1),
                new Word(Opcode.STOP, -1, -1, -1)
        };

        public Word[] bubbleSort = new Word[]{
                new Word(Opcode.DATA, -1, -1, 41),  // jump do primeiro loop gm ou em
                new Word(Opcode.DATA, -1, -1, 9),   // jump do segundo loop gm ou em
                new Word(Opcode.DATA, -1, -1, 34),  // jump do terceiro loop lm
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.LDD, 1, -1, 43),
                new Word(Opcode.LDI, 2, -1, 0),
                new Word(Opcode.LDI, 3, -1, 0),
                new Word(Opcode.LDI, 5, -1, 0),     // linha 9. Início loop 1
                new Word(Opcode.ADD, 5, 2, -1),
                new Word(Opcode.SUB, 5, 1, -1),
                new Word(Opcode.JMPIGM, -1, 5, 0),  // linha 12 - pula pra linha 41 que é o fim (armazenado na memória [0])
                new Word(Opcode.JMPIEM, -1, 5, 0),
                new Word(Opcode.ADDI, 2, -1, 1),
                new Word(Opcode.LDI, 3, -1, 0),     // linha 15
                new Word(Opcode.LDI, 5, -1, 0),
                new Word(Opcode.ADD, 5, 3, -1),
                new Word(Opcode.ADDI, 5, -1, 1),
                new Word(Opcode.SUB, 5, 1, -1),
                new Word(Opcode.JMPIEM, -1, 5, 1),  // linha 20. Pula pra linha 9 (armazenado na memória [1]). Loop 1
                new Word(Opcode.JMPIGM, -1, 5, 1),
                new Word(Opcode.LDI, 4, -1, 44),
                new Word(Opcode.ADD, 4, 3, -1),
                new Word(Opcode.LDI, 5, -1, 1),
                new Word(Opcode.ADD, 5, 4, -1),
                new Word(Opcode.LDX, 4, 4, -1),
                new Word(Opcode.LDX, 5, 5, -1),
                new Word(Opcode.ADDI, 3, -1, 1),
                new Word(Opcode.LDI, 6, -1, 0),
                new Word(Opcode.ADD, 6, 5, -1),
                new Word(Opcode.SUB, 6, 4, -1),
                new Word(Opcode.JMPILM, -1, 6, 2), // linha 32. pula pra linha 34 (amazenado na memória [2])
                new Word(Opcode.JMP, -1, -1, 16),
                new Word(Opcode.SWAP, 5, 4, -1),
                new Word(Opcode.LDI, 6, -1, 43),
                new Word(Opcode.ADD, 6, 3, -1),
                new Word(Opcode.STX, 6, 4, -1),
                new Word(Opcode.ADDI, 6, -1, 1),
                new Word(Opcode.STX, 6, 5, -1),
                new Word(Opcode.JMP, -1, -1, 16),
                new Word(Opcode.STOP, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1), // linha 42. não usada
                new Word(Opcode.DATA, -1, -1, 6), // tamanho do vetor
                new Word(Opcode.DATA, -1, -1, 12), // dados do vetor a partir daqui até o final
                new Word(Opcode.DATA, -1, -1, 7),
                new Word(Opcode.DATA, -1, -1, 9),
                new Word(Opcode.DATA, -1, -1, 1),
                new Word(Opcode.DATA, -1, -1, 4),
                new Word(Opcode.DATA, -1, -1, 3)
        };

        public Word[] trapTestOutput = new Word[]{
                new Word(Opcode.LDI, 1, -1, 44),    //coloca 44 no reg 1
                new Word(Opcode.STD, 1, -1, 6),    // coloca o valor de reg1 na posição 6 da memória
                new Word(Opcode.LDI, 8, -1, 2),     // coloca 2 em reg 8 para criar um trap de out
                new Word(Opcode.LDI, 9,-1,6),      // coloca 6 no reg 9, ou seja a posição onde será feita a leitura
                new Word(Opcode.TRAP,-1,-1,-1),     // faz o output da posição 10
                new Word(Opcode.STOP, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1)
        };

        public Word[] trapTestInput = new Word[]{
                new Word(Opcode.LDI, 8, -1, 1),     // coloca 2 em reg 8 para criar um trap de input
                new Word(Opcode.LDI, 9,-1,4),      // coloca 4 no reg 9, ou seja a posição onde será feita a escrita
                new Word(Opcode.TRAP,-1,-1,-1),     // faz o output da posição 10
                new Word(Opcode.STOP, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1)
        };

    }
}

