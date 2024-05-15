package br.com.alura.screenmatch.service;

import br.com.alura.screenmatch.dto.EpisodioDTO;
import br.com.alura.screenmatch.dto.SerieDTO;
import br.com.alura.screenmatch.model.Categoria;
import br.com.alura.screenmatch.model.Episodio;
import br.com.alura.screenmatch.model.Serie;
import br.com.alura.screenmatch.repository.SerieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class SerieService {

    @Autowired
    private SerieRepository repository;

    public List<SerieDTO> obterTodasSeries(){
        return converteDados(repository.findAll());
    }

    public List<SerieDTO> obterTop5Series() {

        return  converteDados(repository.findTop5ByOrderByAvaliacaoDesc());
    }


    public List<SerieDTO> obterLancamentos() {
        return converteDados(repository.lancamentosMaisRecentes());
    }

    public SerieDTO obterPorId(Long id) {
        Optional<Serie> serie = repository.findById(id);
        if (serie.isPresent())
            return new SerieDTO(serie.get());
        return null;
    }

    public List<EpisodioDTO> obterTodasTemporadas(long id) {
        Optional<Serie> serie = repository.findById(id);
        if (serie.isPresent())
            return serie.get().getEpisodios()
                    .stream()
                    .map(s -> new EpisodioDTO(s.getTemporada(), s.getNumero(), s.getTitulo()))
                    .toList();
        return null;

    }

    public List<EpisodioDTO> obterTemporada(Long id, Integer numeroTemporada) {

        //Com streams
        /*Optional<Serie> serie = repository.findById(id);
        if (serie.isPresent())
            return serie.get().getEpisodios()
                    .stream()
                    .filter(s -> s.getTemporada().equals(numeroTemporada))
                    .map(s -> new EpisodioDTO(s.getTemporada(), s.getNumero(), s.getTitulo()))
                    .toList();
        return null;*/
        List<Episodio> episodios =  repository.obterEpisodiosPorTemporada(id, numeroTemporada);
        return episodios.stream()
                .map(e -> new EpisodioDTO(e.getTemporada(), e.getNumero(), e.getTitulo()))
                .toList();
    }

    public List<SerieDTO> obterSeriesPorCategoria(String genero) {
        Categoria categoria = Categoria.fromStringPtBR(genero);
        return converteDados(repository.findByGenero(categoria));
    }

    private List<SerieDTO> converteDados(List<Serie> series){
        return series.stream()
                .map(SerieDTO::new)
                .toList();
    }
}
