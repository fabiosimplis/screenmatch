package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoAPI;
import br.com.alura.screenmatch.service.ConverteDados;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoAPI consumo = new ConsumoAPI();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=529b3feb";
    
    private List<DadosSerie> dadosSeries = new ArrayList<>();
    private List<Serie> series = new ArrayList<>();
    private SerieRepository repositorio;

    public Principal(SerieRepository repositorio){
        this.repositorio = repositorio;
    }

    public void exibeMenu(){

        var opcao = -1;

        while(opcao != 0){
            var menu = """
                    \n1 - Buscar Séries
                    2 - Buscar Episódios
                    3 - Listar Séries Listadas
                    4 - Buscar Série por Título
                    5 - Buscar Série por Ator
                    6 - Buscar Top 5 Séries
                    7 - Buscar Série por Categoria
                    
                    0 - Sair
                    
                    Digite a opção desejada:
                    """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao){
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscaSeriesPorTitulo();
                    break;
                case 5:
                    buscarSeriePorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarSeriesPorCategoria();
                    break;
                case 0:
                    System.out.println("Saindo ...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }

    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();

        if (dados.titulo() == null){
            System.out.println("Serie não encontrada!!!\n");
            return;
        }
        Serie serie = new Serie(dados);
        dadosSeries.add(dados);
        repositorio.save(serie);

        System.out.println(dados);
    }
    private void buscarEpisodioPorSerie() {
        listarSeriesBuscadas();
        System.out.println("Escolha uma série pelo nome:");
        var nomeSerie = leitura.nextLine();

//        Optional<Serie> serie = series.stream()
//                .filter(s -> s.getTitulo().toLowerCase().contains(nomeSerie.toLowerCase()))
//                .findFirst();

        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serie.isPresent()) {

            var serieEncontrada = serie.get();

            List<DadosTemporada> temporadas = new ArrayList<>();
            System.out.println("Buscando Episódios!!!");
            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                var dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(t -> t.episodios().stream()
                            .map(e -> new Episodio(t.numero(), e)))
                    .collect(Collectors.toList());
            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);

        } else {
            System.out.println("Série não encontrada");
        }
    }

    private void listarSeriesBuscadas() {
        series = repositorio.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscaSeriesPorTitulo() {
        System.out.println("Escolha o título da série:");
        var tituloSerie = leitura.nextLine();
        Optional<Serie> tituloSerieBuscado = repositorio.findByTituloContainingIgnoreCase(tituloSerie);

        if (tituloSerieBuscado.isPresent()){
            Serie serie = tituloSerieBuscado.get();
            System.out.println("\nDados da Série buscada:\n" + serie);
        }else{
            System.out.println("Série não buscada");
        }
    }

    private void buscarSeriePorAtor() {
        System.out.println("Qual nome do ator deseja buscar:");
        var atorBuscado = leitura.nextLine();
        System.out.println("Avaliação a partir de qual valor:");
        var avaliacao = leitura.nextDouble();

        List<Serie> seriesEncontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(atorBuscado, avaliacao);

        System.out.println("\nSeries encontradas:");
        seriesEncontradas.forEach( s ->
                System.out.println(s.getTitulo() + " avaliação: " + s.getAvaliacao()));
    }

    private void buscarTop5Series(){

        List<Serie> top5Series = repositorio.findTop5ByOrderByAvaliacaoDesc();

        top5Series.forEach( s -> System.out.println(s.getTitulo() + ", Avaliação: " + s.getAvaliacao()));
    }
    private void buscarSeriesPorCategoria(){
        System.out.println("Qual categoria deseja busca?");
        var nomeCategoria = leitura.nextLine();
        Categoria categoria = Categoria.fromStringPtBR(nomeCategoria);

        List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);

        System.out.println("\nSéries da categoria " + categoria.name());

        seriesPorCategoria.forEach( s -> System.out.println(s.getTitulo() + ", categoria = " + s.getGenero()));
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);

        return dados;
    }

    private DoubleSummaryStatistics estatisticasDeAvaliacoes(List<Episodio> episodios){

        System.out.println("\n - ESTATISTICAS - \n");

        return episodios.stream()
                .filter( ep -> ep.getAvaliacao() > 0.0)
                .collect(Collectors.summarizingDouble(Episodio::getAvaliacao));
    }

    private void imprimiEstatisticas(DoubleSummaryStatistics est){
        System.out.println("\n - IMPRIMI ESTATISTICAS- \n");
        System.out.println("Média das Notas Por Episódios: " + est.getAverage());
        System.out.println("Melhor nota de episódio: " + est.getMax());
        System.out.println("Pior nota de episódio: " + est.getMin());
        System.out.println("Quantidade de episódios: " + est.getCount());
    }

    private void avaliacoesPorTemporada(List<Episodio> episodios){

        System.out.println("\n - AVALIAÇÃO POR TEMPORADA - \n");

        Map<Integer, Double> avaliacaoPorTemporada = episodios.stream()
                                                        .filter( ep -> ep.getAvaliacao() > 0.0)
                                                        .collect(Collectors.groupingBy(Episodio::getTemporada,
                                                                Collectors.averagingDouble(Episodio::getAvaliacao)));

        System.out.println(avaliacaoPorTemporada);

    }

    private void buscaApatirData(List<Episodio> episodios) {

        System.out.println("\n - BUSCA EPISÓDIOS A PARTIR DE UMA DATA -\n");

        System.out.println("A partir de que ano deseja ver os episódios");
        var ano = leitura.nextInt();
        leitura.nextLine();

        LocalDate dataBusca = LocalDate.of(ano, 1, 1);

        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        System.out.println("\n Busca a partir da data \n");
        episodios.stream()
                .filter(ep -> ep.getDataLancamento() != null && ep.getDataLancamento().isAfter(dataBusca))
                .forEach(e -> System.out.println(
                        "Temporada: " + e.getTemporada() +
                                " Episódio: " + e.getTitulo() +
                                " Data Lançamento: " + e.getDataLancamento().format(formatador)
                ));
    }

    private void buscaEpisodio(List<Episodio> episodios) {
        System.out.println("Qual o título do episódio deseja");
        var trechoTitulo = leitura.nextLine();

        Optional<Episodio> episodioBuscando = episodios.stream()
                .filter(ep -> ep.getTitulo().toUpperCase().contains(trechoTitulo.toUpperCase()))
                .findFirst();

        if (episodioBuscando.isPresent()){
            System.out.println("Episódio encontrado");
            System.out.println(episodioBuscando.get());
        }
        else{
            System.out.println("Episódio não encontrado");
        }
    }


    private static List<Episodio> getEpisodios(List<DadosTemporada> temporadas) {
        List<Episodio> episodios = temporadas.stream()
                .flatMap(t -> t.episodios().stream()
                        .map(episodio -> new Episodio(t.numero(), episodio))
                ).collect(Collectors.toList());
        return episodios;
    }

    private void printQuantidadeDeEpisodios(List<DadosEpisodio> dadosEpisodios, int numeroDeEpisoios) {
        dadosEpisodios.stream()
                .filter(episodio -> !episodio.avaliacao().equalsIgnoreCase("N/A"))
                .peek(e -> System.out.println("Primeiro filtro " + e))
                .sorted(Comparator.comparing(DadosEpisodio::avaliacao).reversed())
                .peek(e -> System.out.println("Ordenacao " + e))
                .limit(5)
                .peek(e -> System.out.println("Mapeamento " + e))
                .forEach(System.out::println);
    }

    private List<DadosEpisodio> getDadosEpisodios(List<DadosTemporada> temporadas) {
        List<DadosEpisodio> dadosEpisodios = temporadas.stream()
                .flatMap(t -> t.episodios().stream())
                .collect(Collectors.toList());
        //.toList(); // Lista imutável, não podendo adionar dados
        return dadosEpisodios;
    }

}
